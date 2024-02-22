package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiDCKafkaBrokerMessageProducer implements BrokerMessageProducer {

    private final Producers producers;
    private final MetricsFacade metricsFacade;
    private final MessageToKafkaProducerRecordConverter messageConverter;
    private final Duration speculativeSendDelay;
    private final RemoteProducerProvider remoteProducerProvider;


    public MultiDCKafkaBrokerMessageProducer(Producers producers,
                                             RemoteProducerProvider remoteProducerProvider,
                                             MetricsFacade metricsFacade,
                                             MessageToKafkaProducerRecordConverter messageConverter,
                                             Duration speculativeSendDelay
                                             ) {
        this.messageConverter = messageConverter;
        this.producers = producers;
        this.metricsFacade = metricsFacade;
        this.speculativeSendDelay = speculativeSendDelay;
        this.remoteProducerProvider = remoteProducerProvider;
    }

    //TODO: single thread is probably not enough because send() can block for a long time
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    @Override
    public void send(Message message, CachedTopic cachedTopic, PublishingCallback callback) {

        var producerRecord = messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        Optional<Producer<byte[], byte[]>> remoteProducer = remoteProducerProvider.get(cachedTopic, producers);

        final ThreadSafeCallback publishingCallback = remoteProducer.isPresent()
                ? ThreadSafeCallback.withFallback(message, cachedTopic, callback)
                : ThreadSafeCallback.withoutFallback(message, cachedTopic, callback);

        scheduler.schedule(() -> {
            if (!publishingCallback.sent.get() && remoteProducer.isPresent()) {
                send(remoteProducer.get(),
                        producerRecord,
                        publishingCallback,
                        cachedTopic,
                        message);
            }
        }, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);

        scheduler.execute(
                () -> send(producers.get(cachedTopic.getTopic()),
                        producerRecord,
                        publishingCallback,
                        cachedTopic,
                        message
                )
        );

    }

    private void send(Producer<byte[], byte[]> producer,
                      ProducerRecord<byte[], byte[]> producerRecord,
                      ThreadSafeCallback callback,
                      CachedTopic cachedTopic,
                      Message message) {
        try {
            producer.send(producerRecord, callback);
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    private static class ThreadSafeCallback implements PublishingCallback, Callback {

        private final Message message;
        private final CachedTopic topic;
        private final PublishingCallback callback;

        private final AtomicBoolean sent = new AtomicBoolean(false);
        private final AtomicInteger tries;

        private ThreadSafeCallback(Message message, CachedTopic topic, PublishingCallback callback, int tries) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
            this.tries = new AtomicInteger(tries);
        }

        static ThreadSafeCallback withFallback(Message message, CachedTopic topic, PublishingCallback callback) {
            return new ThreadSafeCallback(message, topic, callback, 2);
        }

        static ThreadSafeCallback withoutFallback(Message message, CachedTopic topic, PublishingCallback callback) {
            return new ThreadSafeCallback(message, topic, callback, 1);
        }


        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                this.onPublished(message, topic.getTopic());
            } else {
                this.onUnpublished(message, topic.getTopic(), exception);
            }
        }

        @Override
        public void onUnpublished(Message message, Topic topic, Exception exception) {
            if (tries.decrementAndGet() == 0) {
                // TODO: consider reporting exceptions from both DCs, e.g. by caching the first one
                callback.onUnpublished(message, topic, exception);
            }
        }

        @Override
        public void onPublished(Message message, Topic topic) {
            if (sent.compareAndSet(false, true)) {
                callback.onPublished(message, topic);
                // producers.maybeRegisterNodeMetricsGauges(metricsFacade);
            }
            // TODO: consider adding metrics for 'else' case: event duplication
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic topic) {
        return false;
    }
}
