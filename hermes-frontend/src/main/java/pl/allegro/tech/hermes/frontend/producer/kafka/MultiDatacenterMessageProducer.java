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

        if (remoteSender.isPresent()) {
            sendWithFallback(localSender, remoteSender.get(), producerRecord, callback, cachedTopic, message);
        } else {
            sendWithoutFallback(localSender, producerRecord, callback, cachedTopic, message);
        }
    }

    private void sendWithoutFallback(KafkaMessageSender<byte[], byte[]> sender,
                                     ProducerRecord<byte[], byte[]> producerRecord,
                                     PublishingCallback callback,
                                     CachedTopic cachedTopic,
                                     Message message) {
        SendWithoutFallbackCallback sendWithoutFallbackCallback = new SendWithoutFallbackCallback(callback);
        String datacenter = sender.getDatacenter();
        try {
            sender.send(producerRecord, cachedTopic, message, new DCAwareCallback(
                    message,
                    cachedTopic,
                    datacenter,
                    sendWithoutFallbackCallback));
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be sent to a broker
            sendWithoutFallbackCallback.onUnpublished(message, cachedTopic, datacenter, e);
        }
    }

    private void sendWithFallback(KafkaMessageSender<byte[], byte[]> localSender,
                                  KafkaMessageSender<byte[], byte[]> remoteSender,
                                  ProducerRecord<byte[], byte[]> producerRecord,
                                  PublishingCallback callback,
                                  CachedTopic cachedTopic,
                                  Message message) {
        Map<String, ChaosExperiment> experiments = createChaosExperimentsPerDatacenter(cachedTopic.getTopic());

        SendWithFallbackCallback sendWithFallbackCallback = new SendWithFallbackCallback(
                callback,
                experiments.getOrDefault(remoteSender.getDatacenter(), ChaosExperiment.DISABLED),
                remoteSender,
                producerRecord
        );

        fallbackScheduler.schedule(
                () -> sendWithFallbackCallback.onUnpublished(message, cachedTopic, localSender.getDatacenter(), null /* todo */),
                speculativeSendDelay.toMillis(),
                TimeUnit.MILLISECONDS
        );

        sendOrScheduleChaosExperiment(
                localSender,
                producerRecord,
                sendWithFallbackCallback,
                cachedTopic,
                message,
                experiments.getOrDefault(localSender.getDatacenter(), ChaosExperiment.DISABLED)
        );
    }

    private void sendOrScheduleChaosExperiment(KafkaMessageSender<byte[], byte[]> sender,
                                               ProducerRecord<byte[], byte[]> producerRecord,
                                               SendCallback callback,
                                               CachedTopic cachedTopic,
                                               Message message,
                                               ChaosExperiment experiment) {
        if (experiment.enabled()) {
            scheduleChaosExperiment(experiment, sender, producerRecord, callback, cachedTopic, message);
        } else {
            try {
                sender.send(producerRecord, cachedTopic, message, new DCAwareCallback(
                        message,
                        cachedTopic,
                        sender.getDatacenter(),
                        callback));
            } catch (Exception e) {
                // Run fallback immediately after sending to Kafka failed. It will
                // run concurrently with scheduled fallback
                callback.onUnpublished(message, cachedTopic, sender.getDatacenter(), e);
            }
        }
    }

    private void scheduleChaosExperiment(ChaosExperiment experiment,
                                         KafkaMessageSender<byte[], byte[]> sender,
                                         ProducerRecord<byte[], byte[]> producerRecord,
                                         SendCallback callback,
                                         CachedTopic cachedTopic,
                                         Message message) {
        try {
            chaosScheduler.schedule(() -> {
                if (experiment.completeWithError()) {
                    var datacenter = sender.getDatacenter();
                    var exception = new ChaosException(datacenter, experiment.delayInMillis(), message.getId());
                    callback.onUnpublished(message, cachedTopic, datacenter, exception);
                } else {
                    send(sender, producerRecord, callback, cachedTopic, message);
                }
            }, experiment.delayInMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logger.warn("Failed while scheduling chaos experiment. Sending message to Kafka.", e);
            send(sender, producerRecord, callback, cachedTopic, message);
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

    private record ChaosExperiment(boolean enabled, boolean completeWithError, long delayInMillis) {

        private static final ChaosExperiment DISABLED = new ChaosExperiment(false, false, 0);

    }

    private Optional<KafkaMessageSender<byte[], byte[]>> getRemoteSender(CachedTopic cachedTopic) {
        return kafkaMessageSenders.getRemote(cachedTopic.getTopic())
                .stream()
                .filter(producer -> adminReadinessService.isDatacenterReady(producer.getDatacenter()))
                .findFirst();
    }

    private class SendWithFallbackCallback implements SendCallback {

        private final AtomicBoolean sent = new AtomicBoolean(false);
        private final AtomicInteger tries = new AtomicInteger(2);
        private final ConcurrentHashMap<String, Exception> errors = new ConcurrentHashMap<>(2);
        private final PublishingCallback callback;
        private final ChaosExperiment experiment;
        private final KafkaMessageSender<byte[], byte[]> remoteSender;
        private final ProducerRecord<byte[], byte[]> producerRecord;

        SendWithFallbackCallback(PublishingCallback callback,
                                 ChaosExperiment experiment,
                                 KafkaMessageSender<byte[], byte[]> remoteSender,
                                 ProducerRecord<byte[], byte[]> producerRecord) {
            this.callback = callback;
            this.experiment = experiment;
            this.remoteSender = remoteSender;
            this.producerRecord = producerRecord;
        }

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception exception) {
            errors.put(datacenter, exception);
            if (tries.decrementAndGet() == 0) { // todo: <= (?)
                callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(errors));
            } else if (experiment.enabled()) {
                sendOrScheduleChaosExperiment(
                        remoteSender,
                        producerRecord,
                        new SendWithoutFallbackCallback(callback),
                        cachedTopic,
                        message,
                        experiment
                );
            } else {
                sendWithoutFallback(remoteSender, producerRecord, callback, cachedTopic, message);
            }
        }

        @Override
        public void onPublished(Message message, CachedTopic cachedTopic, String datacenter) {
            callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
            if (sent.compareAndSet(false, true)) {
                callback.onPublished(message, cachedTopic.getTopic());
            } else {
                cachedTopic.markMessageDuplicated();
            }
        }
    }

    private record DCAwareCallback(Message message, CachedTopic cachedTopic, String datacenter,
                                   SendCallback callback) implements Callback {

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                callback.onPublished(message, cachedTopic, datacenter);
            } else {
                callback.onUnpublished(message, cachedTopic, datacenter, exception);
            }
        }
    }

    private interface SendCallback {

        void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception exception);

        void onPublished(Message message, CachedTopic cachedTopic, String datacenter);
    }

    private static class SendWithoutFallbackCallback implements SendCallback {

        private final PublishingCallback callback;
        private final AtomicBoolean sent = new AtomicBoolean(false);   // todo: do we need this?
        private final AtomicInteger tries = new AtomicInteger(1);  // todo: do we need this?
        private final ConcurrentHashMap<String, Exception> errors = new ConcurrentHashMap<>(1);   // todo: do we need this?

        private SendWithoutFallbackCallback(PublishingCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception exception) {
            errors.put(datacenter, exception);
            if (tries.decrementAndGet() == 0) {
                callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(errors));
            }
        }

        @Override
        public void onPublished(Message message, CachedTopic cachedTopic, String datacenter) {
            callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
            if (sent.compareAndSet(false, true)) {
                callback.onPublished(message, cachedTopic.getTopic());
            } else {
                cachedTopic.markMessageDuplicated();
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
