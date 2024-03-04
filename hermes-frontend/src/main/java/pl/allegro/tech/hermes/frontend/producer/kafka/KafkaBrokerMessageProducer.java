package pl.allegro.tech.hermes.frontend.producer.kafka;

import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

@Singleton
public class KafkaBrokerMessageProducer implements BrokerMessageProducer {

    private final Producers producers;
    private final MessageToKafkaProducerRecordConverter messageConverter;

    public KafkaBrokerMessageProducer(Producers producers,
                                      MetricsFacade metricsFacade,
                                      MessageToKafkaProducerRecordConverter messageConverter) {
        this.producers = producers;
        this.messageConverter = messageConverter;
        producers.registerGauges(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        ProducerRecord<byte[], byte[]> producerRecord =
                messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        try {
            var producer = producers.get(cachedTopic.getTopic());
            Callback wrappedCallback = new SendCallback(message, cachedTopic, callback, producer.getDatacenter());
            producer.send(producerRecord, cachedTopic, message, wrappedCallback);
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    private class SendCallback implements org.apache.kafka.clients.producer.Callback {

        private final Message message;
        private final CachedTopic topic;
        private final PublishingCallback callback;
        private final String datacenter;

        public SendCallback(Message message, CachedTopic topic, PublishingCallback callback, String datacenter) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
            this.datacenter = datacenter;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e == null) {
                callback.onEachPublished(message, topic.getTopic(), datacenter);
                callback.onPublished(message, topic.getTopic());
            } else {
                callback.onUnpublished(message, topic.getTopic(), e);
            }

        }
    }
}
