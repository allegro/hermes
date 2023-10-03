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
    private final Producers producers;
    private final KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher;
    private final MetricsFacade metricsFacade;
    private final MessageToKafkaProducerRecordConverter messageConverter;

    public KafkaBrokerMessageProducer(Producers producers,
                                      KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                      MetricsFacade metricsFacade,
                                      MessageToKafkaProducerRecordConverter messageConverter) {
        this.producers = producers;
        this.kafkaTopicMetadataFetcher = kafkaTopicMetadataFetcher;
        this.metricsFacade = metricsFacade;
        this.messageConverter = messageConverter;
        producers.registerGauges(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        ProducerRecord<byte[], byte[]> producerRecord =
                messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        try {
            producers.get(cachedTopic.getTopic()).send(producerRecord, new SendCallback(message, cachedTopic, callback));
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic cachedTopic) {
        String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();

        try {
            List<PartitionInfo> partitionInfos = producers.get(cachedTopic.getTopic()).partitionsFor(kafkaTopicName);
            if (anyPartitionWithoutLeader(partitionInfos)) {
                logger.warn("Topic {} has partitions without a leader.", kafkaTopicName);
                return false;
            }
            if (anyUnderReplicatedPartition(partitionInfos, kafkaTopicName)) {
                logger.warn("Topic {} has under replicated partitions.", kafkaTopicName);
                return false;
            }
            if (partitionInfos.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Could not read information about partitions for topic {}. {}", kafkaTopicName, e.getMessage());
            return false;
        }

        logger.warn("No information about partitions for topic {}", kafkaTopicName);
        return false;
    }

    private Supplier<ProduceMetadata> produceMetadataSupplier(CachedTopic topic, RecordMetadata recordMetadata) {
        return () -> {
            String kafkaTopicName = topic.getKafkaTopics().getPrimary().name().asString();
            try {
                List<PartitionInfo> topicPartitions = producers.get(topic.getTopic()).partitionsFor(kafkaTopicName);

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

    private boolean anyPartitionWithoutLeader(List<PartitionInfo> partitionInfos) {
        return partitionInfos.stream().anyMatch(p -> p.leader() == null);
    }

    private boolean anyUnderReplicatedPartition(List<PartitionInfo> partitionInfos, String kafkaTopicName) throws Exception {
        int minInSyncReplicas = kafkaTopicMetadataFetcher.fetchMinInSyncReplicas(kafkaTopicName);
        return partitionInfos.stream().anyMatch(p -> p.inSyncReplicas().length < minInSyncReplicas);
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
                producers.maybeRegisterNodeMetricsGauges(metricsFacade);
            } else {
                callback.onUnpublished(message, topic.getTopic(), produceMetadata, e);
            }
        }
    }
}
