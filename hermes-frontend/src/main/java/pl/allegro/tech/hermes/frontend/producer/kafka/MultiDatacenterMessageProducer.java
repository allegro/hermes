package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class MultiDatacenterMessageProducer implements BrokerMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MultiDatacenterMessageProducer.class);

    private final KafkaMessageSenders kafkaMessageSenders;
    private final MessageToKafkaProducerRecordConverter messageConverter;
    private final Duration speculativeSendDelay;
    private final AdminReadinessService adminReadinessService;
    private final ScheduledExecutorService fallbackScheduler;
    private final ScheduledExecutorService chaosScheduler;

    public MultiDatacenterMessageProducer(KafkaMessageSenders kafkaMessageSenders,
                                          AdminReadinessService adminReadinessService,
                                          MessageToKafkaProducerRecordConverter messageConverter,
                                          Duration speculativeSendDelay,
                                          ScheduledExecutorService fallbackScheduler,
                                          ScheduledExecutorService chaosScheduler) {
        this.messageConverter = messageConverter;
        this.kafkaMessageSenders = kafkaMessageSenders;
        this.speculativeSendDelay = speculativeSendDelay;
        this.adminReadinessService = adminReadinessService;
        this.fallbackScheduler = fallbackScheduler;
        this.chaosScheduler = chaosScheduler;
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, PublishingCallback callback) {
        var producerRecord = messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        KafkaMessageSender<byte[], byte[]> localSender = kafkaMessageSenders.get(cachedTopic.getTopic());
        Optional<KafkaMessageSender<byte[], byte[]>> remoteSender = getRemoteSender(cachedTopic);

        Map<String, ChaosExperiment> experiments = createChaosExperimentsPerDatacenter(cachedTopic.getTopic());

        if (remoteSender.isPresent()) {
            sendWithFallback(
                    localSender,
                    remoteSender.get(),
                    producerRecord,
                    cachedTopic,
                    message,
                    experiments,
                    SendCallback.withFallback(callback)
            );
        } else {
            sendWithoutFallback(
                    localSender,
                    producerRecord,
                    cachedTopic,
                    message,
                    experiments.getOrDefault(localSender.getDatacenter(), ChaosExperiment.DISABLED),
                    SendCallback.withoutFallback(callback)
            );
        }
    }

    private void sendWithFallback(KafkaMessageSender<byte[], byte[]> localSender,
                                  KafkaMessageSender<byte[], byte[]> remoteSender,
                                  ProducerRecord<byte[], byte[]> producerRecord,
                                  CachedTopic cachedTopic,
                                  Message message,
                                  Map<String, ChaosExperiment> experiments,
                                  SendCallback callback) {

        Fallback fallback = new Fallback(
                remoteSender,
                producerRecord,
                cachedTopic,
                message,
                experiments.getOrDefault(remoteSender.getDatacenter(), ChaosExperiment.DISABLED),
                callback);

        try {
            fallbackScheduler.schedule(fallback, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException rejectedExecutionException) {
            logger.warn("Failed to run schedule fallback for message: {}, topic: {}", message, cachedTopic.getQualifiedName(), rejectedExecutionException);
        }


        send(
                localSender,
                producerRecord,
                cachedTopic,
                message,
                experiments.getOrDefault(localSender.getDatacenter(), ChaosExperiment.DISABLED),
                new FallbackAwareCallback(message, cachedTopic, localSender.getDatacenter(), callback, fallback)
        );

    }

    private void sendWithoutFallback(KafkaMessageSender<byte[], byte[]> sender,
                                     ProducerRecord<byte[], byte[]> producerRecord,
                                     CachedTopic cachedTopic,
                                     Message message,
                                     ChaosExperiment experiment,
                                     SendCallback callback) {
        send(
                sender,
                producerRecord,
                cachedTopic,
                message,
                experiment,
                new DCAwareCallback(message, cachedTopic, sender.getDatacenter(), callback)
        );
    }

    private void send(KafkaMessageSender<byte[], byte[]> sender,
                      ProducerRecord<byte[], byte[]> producerRecord,
                      CachedTopic cachedTopic,
                      Message message,
                      ChaosExperiment experiment,
                      OnErrorCallback callback) {
        String datacenter = sender.getDatacenter();
        try {
            sender.send(producerRecord, cachedTopic, message, callback, experiment);
        } catch (Exception e) {
            callback.onUnpublished(message, cachedTopic, datacenter, e);
        }
    }

    private Map<String, ChaosExperiment> createChaosExperimentsPerDatacenter(Topic topic) {
        PublishingChaosPolicy chaos = topic.getChaos();
        return switch (chaos.mode()) {
            case DISABLED -> Map.of();
            case GLOBAL -> {
                Map<String, ChaosExperiment> experiments = new HashMap<>();
                ChaosPolicy policy = chaos.globalPolicy();
                boolean enabled = computeIfShouldBeEnabled(policy);
                for (String datacenter : kafkaMessageSenders.getDatacenters()) {
                    experiments.put(datacenter, createChaosExperimentForDatacenter(policy, enabled));
                }
                yield experiments;
            }
            case DATACENTER -> {
                Map<String, ChaosExperiment> experiments = new HashMap<>();
                Map<String, ChaosPolicy> policies = chaos.datacenterPolicies();
                for (String datacenter : kafkaMessageSenders.getDatacenters()) {
                    ChaosPolicy policy = policies.get(datacenter);
                    boolean enabled = computeIfShouldBeEnabled(policy);
                    experiments.put(datacenter, createChaosExperimentForDatacenter(policy, enabled));
                }
                yield experiments;
            }
        };
    }

    private boolean computeIfShouldBeEnabled(ChaosPolicy policy) {
        if (policy == null) {
            return false;
        }
        return ThreadLocalRandom.current().nextInt(100) < policy.probability();
    }

    private ChaosExperiment createChaosExperimentForDatacenter(ChaosPolicy policy, boolean enabled) {
        if (!enabled) {
            return ChaosExperiment.DISABLED;
        }
        long delayMillisFrom = policy.delayFrom();
        long delayMillisTo = policy.delayTo();
        long delayMillis = ThreadLocalRandom.current().nextLong(delayMillisTo - delayMillisFrom) + delayMillisFrom;
        return new ChaosExperiment(true, policy.completeWithError(), delayMillis);
    }

    public record ChaosExperiment(boolean enabled, boolean completeWithError, long delayInMillis) {

        private static final ChaosExperiment DISABLED = new ChaosExperiment(false, false, 0);

    }

    private Optional<KafkaMessageSender<byte[], byte[]>> getRemoteSender(CachedTopic cachedTopic) {
        return kafkaMessageSenders.getRemote(cachedTopic.getTopic())
                .stream()
                .filter(producer -> adminReadinessService.isDatacenterReady(producer.getDatacenter()))
                .findFirst();
    }

    interface OnErrorCallback extends Callback {
        void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception e);
    }

    private record DCAwareCallback(Message message, CachedTopic cachedTopic, String datacenter,
                                   SendCallback callback) implements OnErrorCallback {

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                callback.onPublished(message, cachedTopic, datacenter);
            } else {
                callback.onUnpublished(message, cachedTopic, datacenter, exception);
            }
        }

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception e) {
            callback.onUnpublished(message, cachedTopic, datacenter, e);
        }
    }

    private class FallbackAwareCallback implements OnErrorCallback {

        private final Message message;
        private final CachedTopic cachedTopic;
        private final String datacenter;
        private final SendCallback callback;
        private final Fallback fallback;

        public FallbackAwareCallback(Message message, CachedTopic cachedTopic, String datacenter, SendCallback callback, Fallback fallback) {
            this.message = message;
            this.cachedTopic = cachedTopic;
            this.datacenter = datacenter;
            this.callback = callback;
            this.fallback = fallback;
        }

        public void fallback() {
            try {
                fallbackScheduler.execute(fallback);
            } catch (RejectedExecutionException rejectedExecutionException) {
                logger.warn("Failed to run immediate fallback for message: {}, topic: {}", message, cachedTopic.getQualifiedName(), rejectedExecutionException);
            }
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                callback.onPublished(message, cachedTopic, datacenter);
            } else {
                fallback();
                callback.onUnpublished(message, cachedTopic, datacenter, exception);
            }
        }

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception e) {
            fallback();
            callback.onUnpublished(message, cachedTopic, datacenter, e);
        }
    }

    private static class SendCallback {

        private final PublishingCallback callback;
        private final AtomicBoolean sent = new AtomicBoolean(false);
        private final AtomicInteger tries;
        private final ConcurrentHashMap<String, Exception> errors;

        private SendCallback(PublishingCallback callback, int tries) {
            this.callback = callback;
            this.tries = new AtomicInteger(tries);
            this.errors = new ConcurrentHashMap<>(tries);
        }

        static SendCallback withFallback(PublishingCallback callback) {
            return new SendCallback(callback, 2);
        }

        static SendCallback withoutFallback(PublishingCallback callback) {
            return new SendCallback(callback, 1);
        }

        private void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception exception) {
            errors.put(datacenter, exception);
            if (tries.decrementAndGet() == 0) {
                callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(errors));
            }
        }

        private void onPublished(Message message, CachedTopic cachedTopic, String datacenter) {
            callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
            if (sent.compareAndSet(false, true)) {
                callback.onPublished(message, cachedTopic.getTopic());
            } else {
                cachedTopic.markMessageDuplicated();
            }
        }
    }

    private class Fallback implements Runnable {
        private final KafkaMessageSender<byte[], byte[]> remoteSender;
        private final ProducerRecord<byte[], byte[]> producerRecord;
        private final CachedTopic cachedTopic;
        private final Message message;
        private final ChaosExperiment experiment;
        private final SendCallback callback;
        private final AtomicBoolean executed = new AtomicBoolean(false);

        public Fallback(KafkaMessageSender<byte[], byte[]> remoteSender,
                        ProducerRecord<byte[], byte[]> producerRecord,
                        CachedTopic cachedTopic,
                        Message message,
                        ChaosExperiment experiment,
                        SendCallback callback
        ) {
            this.remoteSender = remoteSender;
            this.producerRecord = producerRecord;
            this.cachedTopic = cachedTopic;
            this.message = message;
            this.experiment = experiment;
            this.callback = callback;
        }

        public void run() {
            if (executed.compareAndSet(false, true) && !callback.sent.get()) {
                sendWithoutFallback(
                        remoteSender,
                        producerRecord,
                        cachedTopic,
                        message,
                        experiment,
                        callback
                );
            }
        }
    }

    public static class MultiDCPublishException extends RuntimeException {

        public MultiDCPublishException(Map<String, Exception> exceptionsPerDC) {
            super(errorMessage(exceptionsPerDC));
        }

        private static String errorMessage(Map<String, Exception> exceptionsPerDC) {
            StringBuilder builder = new StringBuilder();
            exceptionsPerDC.forEach(
                    (dc, exception) -> builder.append(String.format("[%s]: %s, ", dc, getRootCauseMessage(exception)))
            );
            return builder.toString();
        }
    }

    @Override
    public boolean areAllTopicsAvailable() {
        return kafkaMessageSenders.areAllTopicsAvailable();
    }

    @Override
    public boolean isTopicAvailable(CachedTopic topic) {
        return kafkaMessageSenders.isTopicAvailable(topic);
    }
}
