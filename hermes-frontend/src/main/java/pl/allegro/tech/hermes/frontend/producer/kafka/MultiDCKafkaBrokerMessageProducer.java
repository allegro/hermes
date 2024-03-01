package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class MultiDCKafkaBrokerMessageProducer implements BrokerMessageProducer {

    private final Producers producers;
    private final MessageToKafkaProducerRecordConverter messageConverter;
    private final Duration speculativeSendDelay;
    private final RemoteProducerProvider remoteProducerProvider;
    //TODO: tune number of threads, prevent OOM
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(16);


    public MultiDCKafkaBrokerMessageProducer(Producers producers,
                                             RemoteProducerProvider remoteProducerProvider,
                                             MessageToKafkaProducerRecordConverter messageConverter,
                                             Duration speculativeSendDelay
    ) {
        this.messageConverter = messageConverter;
        this.producers = producers;
        this.speculativeSendDelay = speculativeSendDelay;
        this.remoteProducerProvider = remoteProducerProvider;
        // TODO: producers.registerGauges(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, PublishingCallback callback) {

        var producerRecord = messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        Optional<KafkaProducer<byte[], byte[]>> remoteProducer = remoteProducerProvider.get(cachedTopic, producers);

        final SendCallback sendCallback = remoteProducer.isPresent()
                ? SendCallback.withFallback(callback)
                : SendCallback.withoutFallback(callback);

        scheduler.schedule(() -> {
            if (!sendCallback.sent.get() && remoteProducer.isPresent()) {
                send(remoteProducer.get(),
                        producerRecord,
                        sendCallback,
                        cachedTopic,
                        message);
            }
        }, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);


        send(producers.get(cachedTopic.getTopic()), producerRecord, sendCallback, cachedTopic, message);
    }

    private void send(KafkaProducer<byte[], byte[]> producer,
                      ProducerRecord<byte[], byte[]> producerRecord,
                      SendCallback callback,
                      CachedTopic cachedTopic,
                      Message message) {
        String datacenter = producer.getDatacenter();
        try {
            producer.send(producerRecord, cachedTopic, message, new DCAwareCallback(
                    message,
                    cachedTopic,
                    datacenter,
                    callback));
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic, datacenter, e);
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


    // TODO: maybe implementation this should be moved to KafkaProducer to make it easier for BrokerMessageProducer implementations
    @Override
    public boolean isTopicAvailable(CachedTopic topic) {
        return false;
    }
}
