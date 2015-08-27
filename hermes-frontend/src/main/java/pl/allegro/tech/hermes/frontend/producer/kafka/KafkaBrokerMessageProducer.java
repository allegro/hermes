package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KafkaBrokerMessageProducer implements BrokerMessageProducer {

    private final Producers producers;
    private final KafkaNamesMapper kafkaNamesMapper;

    @Inject
    public KafkaBrokerMessageProducer(Producers producers, HermesMetrics metrics, KafkaNamesMapper kafkaNamesMapper) {
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.producers = producers;
        producers.registerGauges(metrics);
    }

    @Override
    public void send(Message message, Topic topic, final PublishingCallback callback) {
        try {
            ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(kafkaNamesMapper.toKafkaTopicName(topic).asString(), message.getData());
            producers.get(topic).send(producerRecord, new SendCallback(message, topic, callback));
        } catch (Exception e) {
            callback.onUnpublished(message, topic, e);
        }
    }

    private static class SendCallback implements org.apache.kafka.clients.producer.Callback {

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
            }
        }
    }
}
