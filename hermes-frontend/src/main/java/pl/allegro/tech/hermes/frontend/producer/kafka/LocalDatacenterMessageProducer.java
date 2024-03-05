package pl.allegro.tech.hermes.frontend.producer.kafka;

import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;

@Singleton
public class LocalDatacenterMessageProducer implements BrokerMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(LocalDatacenterMessageProducer.class);
    private final KafkaMessageSenders kafkaMessageSenders;
    private final KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher;
    private final MessageToKafkaProducerRecordConverter messageConverter;

    public LocalDatacenterMessageProducer(KafkaMessageSenders kafkaMessageSenders,
                                          KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                          MetricsFacade metricsFacade,
                                          MessageToKafkaProducerRecordConverter messageConverter
                                      ) {
        this.kafkaMessageSenders = kafkaMessageSenders;
        this.kafkaTopicMetadataFetcher = kafkaTopicMetadataFetcher;
        this.messageConverter = messageConverter;
        kafkaMessageSenders.registerLocalSenderMetrics(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        ProducerRecord<byte[], byte[]> producerRecord =
                messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        try {
            var producer = kafkaMessageSenders.get(cachedTopic.getTopic());
            Callback wrappedCallback = new SendCallback(message, cachedTopic, callback, producer.getDatacenter());
            producer.send(producerRecord, cachedTopic, message, wrappedCallback);
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic cachedTopic) {
        String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();

        try {
            List<PartitionInfo> partitionInfos = kafkaMessageSenders.get(cachedTopic.getTopic()).partitionsFor(kafkaTopicName);
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
