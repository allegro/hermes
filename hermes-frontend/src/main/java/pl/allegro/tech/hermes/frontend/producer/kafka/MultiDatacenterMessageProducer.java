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

    public MultiDatacenterMessageProducer(KafkaMessageSenders kafkaMessageSenders,
                                          AdminReadinessService adminReadinessService,
                                          MessageToKafkaProducerRecordConverter messageConverter,
                                          Duration speculativeSendDelay,
                                          ScheduledExecutorService fallbackScheduler) {
        this.messageConverter = messageConverter;
        this.kafkaMessageSenders = kafkaMessageSenders;
        this.speculativeSendDelay = speculativeSendDelay;
        this.adminReadinessService = adminReadinessService;
        this.fallbackScheduler = fallbackScheduler;
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
                    callback
            );
        } else {
            sendWithoutFallback(
                    localSender,
                    producerRecord,
                    cachedTopic,
                    message,
                    callback
            );
        }
    }

    private class SendWithFallbackExecutionContext {

        private final AtomicBoolean executed = new AtomicBoolean(false);
        private final AtomicBoolean sent = new AtomicBoolean(false);
        private final AtomicInteger tries;
        private final ConcurrentHashMap<String, Exception> errors;

        private SendWithFallbackExecutionContext() {
            this.tries = new AtomicInteger(2);
            this.errors = new ConcurrentHashMap<>(2);
        }

        public boolean acquireExecute() {
            return executed.compareAndSet(false, true) && !sent.get();
        }

        boolean acquireTry(String datacenter, Exception exception) {
            errors.put(datacenter, exception);
            return tries.decrementAndGet() == 0;
        }

        public boolean acquireSend() {
            return sent.compareAndSet(false, true);
        }
    }

    private void sendWithFallback(KafkaMessageSender<byte[], byte[]> localSender,
                                  KafkaMessageSender<byte[], byte[]> remoteSender,
                                  ProducerRecord<byte[], byte[]> producerRecord,
                                  CachedTopic cachedTopic,
                                  Message message,
                                  Map<String, ChaosExperiment> experiments,
                                  PublishingCallback publishingCallback) {

        SendWithFallbackExecutionContext context = new SendWithFallbackExecutionContext();

        FallbackRunnable fallback = new FallbackRunnable(
                remoteSender,
                producerRecord,
                cachedTopic,
                message,
                experiments.getOrDefault(remoteSender.getDatacenter(), ChaosExperiment.DISABLED),
                publishingCallback,
                context);

        Future<?> scheduledFallback;
        try {
            scheduledFallback = fallbackScheduler.schedule(fallback, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException rejectedExecutionException) {
            logger.warn("Failed to run schedule fallback for message: {}, topic: {}", message, cachedTopic.getQualifiedName(), rejectedExecutionException);
            scheduledFallback = CompletableFuture.completedFuture(null);
        }

        send(
                localSender,
                producerRecord,
                cachedTopic,
                message,
                experiments.getOrDefault(localSender.getDatacenter(), ChaosExperiment.DISABLED),
                new FallbackAwareLocalSendCallback(message, cachedTopic, localSender.getDatacenter(), context, publishingCallback,
                        fallback, scheduledFallback)
        );

    }

    private void sendWithoutFallback(KafkaMessageSender<byte[], byte[]> sender,
                                     ProducerRecord<byte[], byte[]> producerRecord,
                                     CachedTopic cachedTopic,
                                     Message message,
                                     PublishingCallback callback) {
        send(
                sender,
                producerRecord,
                cachedTopic,
                message,
                ChaosExperiment.DISABLED,
                new LocalSendCallback(message, cachedTopic, sender.getDatacenter(), callback)
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

    private record RemoteSendCallback(Message message, CachedTopic cachedTopic, String datacenter,
                                      PublishingCallback callback, SendWithFallbackExecutionContext context) implements OnErrorCallback {

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
                if (context.acquireSend()) {
                    callback.onPublished(message, cachedTopic.getTopic());
                } else {
                    cachedTopic.markMessageDuplicated();
                }
            } else {
                if (context.acquireTry(datacenter, exception)) {
                    callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(context.errors));
                }
            }
        }

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception e) {
            if (context.acquireTry(datacenter, e)) {
                callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(context.errors));
            }
        }
    }

    private class FallbackAwareLocalSendCallback implements OnErrorCallback {

        private final Message message;
        private final CachedTopic cachedTopic;
        private final String datacenter;
        private final PublishingCallback callback;
        private final FallbackRunnable fallback;
        private final Future<?> scheduledFallback;
        private final SendWithFallbackExecutionContext context;

        public FallbackAwareLocalSendCallback(Message message, CachedTopic cachedTopic, String datacenter,
                                              SendWithFallbackExecutionContext context,
                                              PublishingCallback callback,
                                              FallbackRunnable fallback, Future<?> scheduledFallback) {
            this.message = message;
            this.cachedTopic = cachedTopic;
            this.datacenter = datacenter;
            this.callback = callback;
            this.fallback = fallback;
            this.scheduledFallback = scheduledFallback;
            this.context = context;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                cancel();
                callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
                if (context.acquireSend()) {
                    callback.onPublished(message, cachedTopic.getTopic());
                } else {
                    cachedTopic.markMessageDuplicated();
                }
            } else {
                fallback();
                if (context.acquireTry(datacenter, exception)) {
                    callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(context.errors));
                }
            }
        }

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception e) {
            fallback();
            if (context.acquireTry(datacenter, e)) {
                callback.onUnpublished(message, cachedTopic.getTopic(), new MultiDCPublishException(context.errors));
            }
        }

        private void fallback() {
            try {
                cancel();
                fallbackScheduler.execute(fallback);
            } catch (RejectedExecutionException rejectedExecutionException) {
                logger.warn("Failed to run immediate fallback for message: {}, topic: {}", message, cachedTopic.getQualifiedName(), rejectedExecutionException);
            }
        }

        private void cancel() {
            scheduledFallback.cancel(false);
        }
    }

    private record LocalSendCallback(Message message, CachedTopic cachedTopic, String datacenter,
                                     PublishingCallback callback) implements OnErrorCallback {

        @Override
        public void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception e) {
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception != null) {
                callback.onUnpublished(message, cachedTopic.getTopic(), exception);
            } else {
                callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
                callback.onPublished(message, cachedTopic.getTopic());
            }
        }
    }

    private class FallbackRunnable implements Runnable {
        private final KafkaMessageSender<byte[], byte[]> remoteSender;
        private final ProducerRecord<byte[], byte[]> producerRecord;
        private final CachedTopic cachedTopic;
        private final Message message;
        private final ChaosExperiment experiment;
        private final PublishingCallback callback;
        private final SendWithFallbackExecutionContext context;

        public FallbackRunnable(KafkaMessageSender<byte[], byte[]> remoteSender,
                                ProducerRecord<byte[], byte[]> producerRecord,
                                CachedTopic cachedTopic,
                                Message message,
                                ChaosExperiment experiment,
                                PublishingCallback callback,
                                SendWithFallbackExecutionContext context
        ) {
            this.remoteSender = remoteSender;
            this.producerRecord = producerRecord;
            this.cachedTopic = cachedTopic;
            this.message = message;
            this.experiment = experiment;
            this.callback = callback;
            this.context = context;
        }

        public void run() {
            if (context.acquireExecute()) {
                send(
                        remoteSender,
                        producerRecord,
                        cachedTopic,
                        message,
                        experiment,
                        new RemoteSendCallback(message, cachedTopic, remoteSender.getDatacenter(), callback, context)
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
