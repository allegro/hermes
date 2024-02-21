package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// 3. Time budget for local. On timeout, fallback to remote.
// - start by sending produce request to local DC
// - after X ms send produce request to remote DC
// - complete hermes client request when at least one DC responded or the global (Y) timeout elapsed
// - X = 250 ms (?); Y = 500 ms (?)
public class KafkaBrokerMessageProducer3 implements BrokerMessageProducer {

    // todo:
    // - set timeouts on producers (globalRequestTimeout for local, globalRequestTimeout - localRequestTimeout for remote)
    // - set globalRequestTimeout for the entire HTTP request

    // Y
    private final Duration globalRequestTimeout;
    // X
    private final Duration localRequestTimeout;
    private final Producers producers;
    private final MessageToKafkaProducerRecordConverter messageConverter;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void send(Message message, CachedTopic cachedTopic, PublishingCallback callback) {
        var producerRecord = messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        final SinglePublishingCallback publishingCallback = new SinglePublishingCallback(callback);

        scheduler.schedule(() -> {
            if (!publishingCallback.sent.get()) {
                send(producers.getRemote(cachedTopic.getTopic()).get(0),
                        producerRecord,
                        callback,
                        cachedTopic,
                        message
                );
            }
        }, localRequestTimeout.toMillis(), TimeUnit.MILLISECONDS);

        send(producers.get(cachedTopic.getTopic()),
                producerRecord,
                callback,
                cachedTopic,
                message
        );
    }

    private void send(Producer<byte[], byte[]> producer,
                      ProducerRecord<byte[], byte[]> producerRecord,
                      PublishingCallback callback,
                      CachedTopic cachedTopic,
                      Message message) {
        try {
            final SendCallback sendCallback = new SendCallback(message, cachedTopic, callback);
            producer.send(producerRecord, sendCallback);
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic topic) {
        return false;
    }

    // - pierwsze DC odpowiedziało sukcesem
    // - oba sfailowały
    // -
    private class SinglePublishingCallback implements PublishingCallback {

        private final PublishingCallback delegate;
        private final AtomicBoolean sent = new AtomicBoolean(false);

        private SinglePublishingCallback(PublishingCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onUnpublished(Message message, Topic topic, Exception exception) {
            delegate.onUnpublished(message, topic, exception);
        }

        @Override
        public void onPublished(Message message, Topic topic) {

        }
    }



    private class SendCallback implements org.apache.kafka.clients.producer.Callback {

        private final Message message;
        private final CachedTopic topic;
        private final PublishingCallback callback;

        public SendCallback(Message message, CachedTopic topic, PublishingCallback callback) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e == null) {
                callback.onPublished(message, topic.getTopic());
//                producers.maybeRegisterNodeMetricsGauges(metricsFacade);
            } else {
                callback.onUnpublished(message, topic.getTopic(), e);
            }
        }
    }
}
