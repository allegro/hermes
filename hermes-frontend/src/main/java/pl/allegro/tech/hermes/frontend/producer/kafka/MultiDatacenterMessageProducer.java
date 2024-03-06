package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class MultiDatacenterMessageProducer implements BrokerMessageProducer {

    private final KafkaMessageSenders kafkaMessageSenders;
    private final MessageToKafkaProducerRecordConverter messageConverter;
    private final Duration speculativeSendDelay;
    private final AdminReadinessService adminReadinessService;
    //TODO: tune number of threads, prevent OOM
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(16);


    public MultiDatacenterMessageProducer(KafkaMessageSenders kafkaMessageSenders,
                                          AdminReadinessService adminReadinessService,
                                          MessageToKafkaProducerRecordConverter messageConverter,
                                          Duration speculativeSendDelay
    ) {
        this.messageConverter = messageConverter;
        this.kafkaMessageSenders = kafkaMessageSenders;
        this.speculativeSendDelay = speculativeSendDelay;
        this.adminReadinessService = adminReadinessService;
        // TODO: producers.registerGauges(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, PublishingCallback callback) {

        var producerRecord = messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        Optional<KafkaMessageSender<byte[], byte[]>> remoteSender = getRemoteSender(cachedTopic);

        final SendCallback sendCallback = remoteSender.isPresent()
                ? SendCallback.withFallback(callback)
                : SendCallback.withoutFallback(callback);

        scheduler.schedule(() -> {
            if (!sendCallback.sent.get() && remoteSender.isPresent()) {
                send(remoteSender.get(),
                        producerRecord,
                        sendCallback,
                        cachedTopic,
                        message);
            }
        }, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);


        send(kafkaMessageSenders.get(cachedTopic.getTopic()), producerRecord, sendCallback, cachedTopic, message);
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
            // message didn't get to internal producer buffer and it will not be send to a broker
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
        private final AtomicReferenceArray<DatacenterError> errors;

        private SendCallback(PublishingCallback callback, int tries) {
            this.callback = callback;
            this.tries = new AtomicInteger(tries);
            this.errors = new AtomicReferenceArray<>(tries);
        }

        static SendCallback withFallback(PublishingCallback callback) {
            return new SendCallback(callback, 2);
        }

        static SendCallback withoutFallback(PublishingCallback callback) {
            return new SendCallback(callback, 1);
        }

        // guarantees that 'onUnpublished' will be called exactly once but two concurrent calls may result
        // it to be called with one error only
        private void onUnpublished(Message message, CachedTopic cachedTopic, String datacenter, Exception exception) {
            int triesLeft = tries.decrementAndGet();
            errors.set(Math.max(triesLeft, 0), new DatacenterError(datacenter, exception));
            if (triesLeft == 0) {
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

    private record DatacenterError(String datacenter, Exception e) {}

    public static class MultiDCPublishException extends RuntimeException {

        private MultiDCPublishException(AtomicReferenceArray<DatacenterError> errors) {
            super(errorMessage(errors));
        }

        private static String errorMessage(AtomicReferenceArray<DatacenterError> errors) {
            StringBuilder builder = new StringBuilder();
            for (var i = 0; i < errors.length(); i++) {
                var error = errors.get(i);
                if (error != null) {
                    builder.append(String.format("[%s]: %s, ", error.datacenter, getRootCauseMessage(error.e)));
                }
            }
            return builder.toString();
        }
    }


    // TODO: maybe implementation this should be moved to KafkaProducer to make it easier for BrokerMessageProducer implementations
    @Override
    public boolean isTopicAvailable(CachedTopic topic) {
        return false;
    }
}
