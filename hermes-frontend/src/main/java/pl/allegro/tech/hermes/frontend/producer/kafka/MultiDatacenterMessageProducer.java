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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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
            sendWithFallback(localSender, remoteSender.get(), producerRecord, SendCallback.withFallback(callback), cachedTopic, message, experiments);
        } else {
            send(
                    localSender,
                    producerRecord,
                    SendCallback.withoutFallback(callback),
                    cachedTopic,
                    message
            );
        }
    }

    private void send(KafkaMessageSender<byte[], byte[]> sender,
                      ProducerRecord<byte[], byte[]> producerRecord,
                      SendCallback callback,
                      CachedTopic cachedTopic,
                      Message message) {
        String datacenter = sender.getDatacenter();
        try {
            sender.send(producerRecord, cachedTopic, message, new DCAwareCallback(
                    message,
                    cachedTopic,
                    datacenter,
                    callback));
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be sent to a broker
            callback.onUnpublished(message, cachedTopic, datacenter, e);
        }
    }

    private void sendWithFallback(KafkaMessageSender<byte[], byte[]> localSender,
                                  KafkaMessageSender<byte[], byte[]> remoteSender,
                                  ProducerRecord<byte[], byte[]> producerRecord,
                                  SendCallback callback,
                                  CachedTopic cachedTopic,
                                  Message message,
                                  Map<String, ChaosExperiment> experiments) {
        Fallback fallback = new Fallback(
                remoteSender,
                producerRecord,
                cachedTopic,
                message,
                experiments.getOrDefault(remoteSender.getDatacenter(), ChaosExperiment.DISABLED),
                callback);
        fallback.schedule(speculativeSendDelay);

        sendWithFallbackOrScheduleChaosExperiment(
                localSender,
                producerRecord,
                callback,
                cachedTopic,
                message,
                experiments.getOrDefault(localSender.getDatacenter(), ChaosExperiment.DISABLED),
                fallback);
    }

    private void sendWithFallback(KafkaMessageSender<byte[], byte[]> localSender,
                                  ProducerRecord<byte[], byte[]> producerRecord,
                                  SendCallback callback,
                                  CachedTopic cachedTopic,
                                  Message message,
                                  Fallback fallback) {
        String localDatacenter = localSender.getDatacenter();

        try {
            localSender.send(producerRecord, cachedTopic, message, new FallbackAwareCallback(
                    message,
                    cachedTopic,
                    localDatacenter,
                    callback,
                    fallback));
        } catch (Exception e) {
            // Run fallback immediately after sending to Kafka failed. It will
            // run concurrently with scheduled fallback
            fallback.runNow();
            callback.onUnpublished(message, cachedTopic, localDatacenter, e);
        }
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
            send(sender, producerRecord, callback, cachedTopic, message);
        }
    }

    private void sendWithFallbackOrScheduleChaosExperiment(KafkaMessageSender<byte[], byte[]> sender,
                                                           ProducerRecord<byte[], byte[]> producerRecord,
                                                           SendCallback callback,
                                                           CachedTopic cachedTopic,
                                                           Message message,
                                                           ChaosExperiment experiment,
                                                           Fallback fallback) {
        if (experiment.enabled()) {
            scheduleChaosExperimentWithFallback(experiment, sender, producerRecord, callback, cachedTopic, message, fallback);
        } else {
            sendWithFallback(sender, producerRecord, callback, cachedTopic, message, fallback);
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

    private void scheduleChaosExperimentWithFallback(ChaosExperiment experiment,
                                                     KafkaMessageSender<byte[], byte[]> sender,
                                                     ProducerRecord<byte[], byte[]> producerRecord,
                                                     SendCallback callback,
                                                     CachedTopic cachedTopic,
                                                     Message message,
                                                     Fallback fallback) {
        try {
            chaosScheduler.schedule(() -> {
                if (experiment.completeWithError()) {
                    var datacenter = sender.getDatacenter();
                    var exception = new ChaosException(datacenter, experiment.delayInMillis(), message.getId());
                    callback.onUnpublished(message, cachedTopic, datacenter, exception);
                    fallback.runNow();
                } else {
                    sendWithFallback(sender, producerRecord, callback, cachedTopic, message, fallback);
                }
            }, experiment.delayInMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logger.warn("Failed while scheduling chaos experiment. Sending message to Kafka.", e);
            sendWithFallback(sender, producerRecord, callback, cachedTopic, message, fallback);
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

    private class Fallback {
        private final KafkaMessageSender<byte[], byte[]> remoteSender;
        private final ProducerRecord<byte[], byte[]> producerRecord;
        private final CachedTopic cachedTopic;
        private final Message message;
        private final ChaosExperiment experiment;
        private final SendCallback callback;
        private final AtomicBoolean executed = new AtomicBoolean(false);
        private volatile Future<?> scheduledExecutionHandle = CompletableFuture.completedFuture(null);

        public Fallback(KafkaMessageSender<byte[], byte[]> remoteSender, ProducerRecord<byte[], byte[]> producerRecord, CachedTopic cachedTopic, Message message, ChaosExperiment experiment, SendCallback callback) {
            this.remoteSender = remoteSender;
            this.producerRecord = producerRecord;
            this.cachedTopic = cachedTopic;
            this.message = message;
            this.experiment = experiment;
            this.callback = callback;
        }

        private void run() {
            if (executed.compareAndSet(false, true) && !callback.sent.get()) {
                sendOrScheduleChaosExperiment(
                        remoteSender,
                        producerRecord,
                        callback,
                        cachedTopic,
                        message,
                        experiment
                );
            }
        }

        public void runNow() {
            try {
                cancelScheduled();
                fallbackScheduler.execute(this::run);
            } catch (RejectedExecutionException rejectedExecutionException) {
                logger.warn("Failed to run immediate fallback for message: {}, topic: {}", message, cachedTopic.getQualifiedName(), rejectedExecutionException);
            }
        }


        public void schedule(Duration delay) {
            try {
                scheduledExecutionHandle = fallbackScheduler.schedule(this::run, delay.toMillis(), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejectedExecutionException) {
                logger.warn("Failed to run schedule fallback for message: {}, topic: {}", message, cachedTopic.getQualifiedName(), rejectedExecutionException);
            }
        }

        // if the scheduled fallback has not yet run - cancel it
        public void cancelScheduled() {
            scheduledExecutionHandle.cancel(false);
        }
    }

    private record FallbackAwareCallback(Message message, CachedTopic cachedTopic, String datacenter,
                                         SendCallback callback, Fallback fallback) implements Callback {

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                callback.onPublished(message, cachedTopic, datacenter);
                fallback.cancelScheduled();
            } else {
                // Run fallback immediately after sending to Kafka failed. It will
                // run concurrently with scheduled fallback
                fallback.runNow();
                callback.onUnpublished(message, cachedTopic, datacenter, exception);
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
