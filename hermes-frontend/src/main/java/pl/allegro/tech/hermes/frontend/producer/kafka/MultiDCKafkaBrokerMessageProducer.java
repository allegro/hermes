package pl.allegro.tech.hermes.frontend.producer.kafka;

import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class MultiDCKafkaBrokerMessageProducer implements BrokerMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MultiDCKafkaBrokerMessageProducer.class);
    private final Producers producers;
    private final KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher;
    private final MetricsFacade metricsFacade;
    private final MessageToKafkaProducerRecordConverter messageConverter;
    private final BrokerLatencyReporter brokerLatencyReporter;

    public MultiDCKafkaBrokerMessageProducer(Producers producers,
                                             KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                             MetricsFacade metricsFacade,
                                             MessageToKafkaProducerRecordConverter messageConverter,
                                             BrokerLatencyReporter brokerLatencyReporter) {
        this.producers = producers;
        this.kafkaTopicMetadataFetcher = kafkaTopicMetadataFetcher;
        this.metricsFacade = metricsFacade;
        this.messageConverter = messageConverter;
        this.brokerLatencyReporter = brokerLatencyReporter;

        // check that remote exists
//        producers.registerGauges(metricsFacade);
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        ProducerRecord<byte[], byte[]> producerRecord =
                messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        var timer = cachedTopic.startBrokerLatencyTimer();
        try {
            var producer = producers.get(cachedTopic.getTopic());
            producer.send(producerRecord, (metadata, exception) -> {
                brokerLatencyReporter.report(timer, message, cachedTopic.getTopic().getAck(), produceMetadataSupplier(cachedTopic, metadata, producer));
                if (exception == null) {
                    callback.onPublished(message, cachedTopic.getTopic());
                } else {
                    // fallback to remote datacenter
                    sendToRemote(producerRecord, message, cachedTopic, callback);
                }
            });
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be sent to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    private void sendToRemote(ProducerRecord<byte[], byte[]> producerRecord, Message message, CachedTopic cachedTopic, PublishingCallback callback) {
        var timer = cachedTopic.startBrokerLatencyTimer();
        try {
            var producer = producers.getRemote(cachedTopic.getTopic()).get(0); // use policy in the future
            producer.send(producerRecord, (metadata, exception) -> {
                brokerLatencyReporter.report(timer, message, cachedTopic.getTopic().getAck(), produceMetadataSupplier(cachedTopic, metadata, producer));
                if (exception == null) {
                    callback.onPublished(message, cachedTopic.getTopic());
                } else {
                    callback.onUnpublished(message, cachedTopic.getTopic(), exception);
                }
            });
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be sent to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    private Supplier<ProduceMetadata> produceMetadataSupplier(CachedTopic topic, RecordMetadata recordMetadata, Producer<byte[], byte[]> producer) {
        return () -> {
            String kafkaTopicName = topic.getKafkaTopics().getPrimary().name().asString();
            try {
                List<PartitionInfo> topicPartitions = producer.partitionsFor(kafkaTopicName);

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
    public boolean isTopicAvailable(CachedTopic cachedTopic) {
        return true;
    }
}
