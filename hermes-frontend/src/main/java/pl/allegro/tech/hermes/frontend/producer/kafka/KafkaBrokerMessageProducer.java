package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapperProvider;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KafkaBrokerMessageProducer implements BrokerMessageProducer {
    
    private final Producers producers;
    private final MessageContentWrapperProvider contentWrapperProvider;
    private final HermesMetrics metrics;

    @Inject
    public KafkaBrokerMessageProducer(Producers producers, MessageContentWrapperProvider contentWrapperProvider, HermesMetrics metrics) {
        this.producers = producers;
        this.contentWrapperProvider = contentWrapperProvider;
        this.metrics = metrics;
        producers.registerGauges(metrics);
    }

    @Override
    public void send(Message message, Topic topic, final PublishingCallback callback) {
        try {
            byte[] content = contentWrapperProvider
                .provide(topic.getContentType())
                .wrapContent(message.getData(), message.getId(), message.getTimestamp());
            ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(topic.getQualifiedName(), content);
            producers.get(topic).send(producerRecord, new SendCallback(message, topic, callback));
        } catch (Exception e) {
            callback.onUnpublished(message, topic, e);
        }
    }

    private class SendCallback implements org.apache.kafka.clients.producer.Callback {
        private final Message message;
        private final Topic topic;
        private final PublishingCallback callback;

        public SendCallback(Message message, Topic topic, PublishingCallback callback) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                callback.onUnpublished(message, topic, e);
            } else {
                callback.onPublished(message, topic);
                producers.maybeRegisterNodeMetricsGauges(metrics);
            }
        }
    }
}
