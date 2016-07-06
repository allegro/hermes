package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KafkaBrokerMessageProducer implements BrokerMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerMessageProducer.class);
    private final Producers producers;
    private final HermesMetrics metrics;

    @Inject
    public KafkaBrokerMessageProducer(Producers producers, HermesMetrics metrics) {
        this.producers = producers;
        this.metrics = metrics;
        producers.registerGauges(metrics);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();
        ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(kafkaTopicName, message.getData());
        try {
            producers.get(cachedTopic.getTopic()).send(producerRecord, new SendCallback(message, cachedTopic.getTopic(), callback));
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic cachedTopic) {
        String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();

        try {
            if (producers.get(cachedTopic.getTopic()).partitionsFor(kafkaTopicName).size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Could not read information about partitions for topic {}. {}", kafkaTopicName, e.getMessage());
            return false;
        }

        logger.warn("No information about partitions for topic {}", kafkaTopicName);
        return false;
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
            if (e == null) {
                callback.onPublished(message, topic);
                producers.maybeRegisterNodeMetricsGauges(metrics);
            } else {
                callback.onUnpublished(message, topic, e);
            }
        }
    }
}
