package pl.allegro.tech.hermes.frontend.producer.kafka;

import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class KafkaBrokerMessageProducer implements BrokerMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerMessageProducer.class);
    private final KafkaMessageSenders kafkaMessageSenders;
    private final MessageToKafkaProducerRecordConverter messageConverter;

    public KafkaBrokerMessageProducer(KafkaMessageSenders kafkaMessageSenders,
                                      MetricsFacade metricsFacade,
                                      MessageToKafkaProducerRecordConverter messageConverter) {
        this.kafkaMessageSenders = kafkaMessageSenders;
        this.messageConverter = messageConverter;
        kafkaMessageSenders.registerLocalSenderMetrics(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        ProducerRecord<byte[], byte[]> producerRecord =
                messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        try {
            var producer = kafkaMessageSenders.get(cachedTopic.getTopic());
            producer.send(producerRecord, new SendCallback(message, cachedTopic, callback));
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    private Supplier<ProduceMetadata> produceMetadataSupplier(CachedTopic topic, RecordMetadata recordMetadata) {
        return () -> {
            String kafkaTopicName = topic.getKafkaTopics().getPrimary().name().asString();
            try {
                List<PartitionInfo> topicPartitions = kafkaMessageSenders.get(topic.getTopic()).loadPartitionMetadataFor(kafkaTopicName);

                Optional<PartitionInfo> partitionInfo = topicPartitions.stream()
                        .filter(p -> p.partition() == recordMetadata.partition())
                        .findFirst();

                return partitionInfo.map(partition -> partition.leader().host())
                        .map(ProduceMetadata::new)
                        .orElse(ProduceMetadata.empty());
            } catch (InterruptException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Could not read information about partitions for topic {}. {}", kafkaTopicName, e.getMessage());
            }
            return ProduceMetadata.empty();
        };
    }

    @Override
    public boolean areAllTopicsAvailable() {
        return kafkaMessageSenders.areAllTopicsAvailable();
    }

    @Override
    public boolean isTopicAvailable(CachedTopic cachedTopic) {
        return kafkaMessageSenders.isTopicAvailable(cachedTopic);
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
            Supplier<ProduceMetadata> produceMetadata = produceMetadataSupplier(topic, recordMetadata);
            if (e == null) {
                callback.onPublished(message, topic.getTopic(), produceMetadata);
            } else {
                callback.onUnpublished(message, topic.getTopic(), produceMetadata, e);
            }
        }
    }
}
