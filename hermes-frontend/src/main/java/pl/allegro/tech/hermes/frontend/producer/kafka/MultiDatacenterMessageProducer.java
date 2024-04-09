package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;

import java.time.Duration;
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

        Optional<KafkaMessageSender<byte[], byte[]>> remoteSender = getRemoteSender(cachedTopic);

        final SendCallback sendCallback = remoteSender.isPresent()
                ? SendCallback.withFallback(callback)
                : SendCallback.withoutFallback(callback);

        fallbackScheduler.schedule(() -> {
            if (!sendCallback.sent.get() && remoteSender.isPresent()) {
                sendOrScheduleChaosExperiment(
                        remoteSender.get(),
                        producerRecord,
                        sendCallback,
                        cachedTopic,
                        message);
            }
        }, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);

        sendOrScheduleChaosExperiment(
                kafkaMessageSenders.get(cachedTopic.getTopic()),
                producerRecord,
                sendCallback,
                cachedTopic,
                message
        );
    }

    private void sendOrScheduleChaosExperiment(KafkaMessageSender<byte[], byte[]> sender,
                                               ProducerRecord<byte[], byte[]> producerRecord,
                                               SendCallback callback,
                                               CachedTopic cachedTopic,
                                               Message message) {
        var chaos = cachedTopic.getTopic().getChaos();
        if (chaos.enabled()) {
            var datacenterChaosPolicies = chaos.datacenterChaosPolicies();
            var policy = datacenterChaosPolicies.get(sender.getDatacenter());
            if (policy != null) {
                scheduleChaosExperiment(policy, sender, producerRecord, callback, cachedTopic, message);
            } else {
                send(sender, producerRecord, callback, cachedTopic, message);
            }
        } else {
            send(sender, producerRecord, callback, cachedTopic, message);
        }
    }

    private void scheduleChaosExperiment(PublishingChaosPolicy.DatacenterChaosPolicy policy,
                                         KafkaMessageSender<byte[], byte[]> sender,
                                         ProducerRecord<byte[], byte[]> producerRecord,
                                         SendCallback callback,
                                         CachedTopic cachedTopic,
                                         Message message) {
        long delayMillisTo = policy.delayTo();
        long delayMillisFrom = policy.delayFrom();
        long delayMillis = ThreadLocalRandom.current().nextLong(delayMillisTo - delayMillisFrom) + delayMillisTo;

        try {
            chaosScheduler.schedule(() -> {
                if (policy.completeWithError()) {
                    var datacenter = sender.getDatacenter();
                    callback.onUnpublished(message, cachedTopic, datacenter, new ChaosException(datacenter, delayMillis, message.getId()));
                } else {
                    send(sender, producerRecord, callback, cachedTopic, message);
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logger.warn("Failed while scheduling chaos experiment. Sending message to Kafka.", e);
            send(sender, producerRecord, callback, cachedTopic, message);
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

    private Optional<KafkaMessageSender<byte[], byte[]>> getRemoteSender(CachedTopic cachedTopic) {
        return kafkaMessageSenders.getRemote(cachedTopic.getTopic())
                .stream()
                .filter(producer -> adminReadinessService.isDatacenterReady(producer.getDatacenter()))
                .findFirst();
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
