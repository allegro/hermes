package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class KafkaMessageSender<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageSender.class);

    private final Producer<K, V> producer;
    private final BrokerLatencyReporter brokerLatencyReporter;
    private final String datacenter;

    public KafkaMessageSender(Producer<K, V> kafkaProducer, BrokerLatencyReporter brokerLatencyReporter, String datacenter) {
        this.producer = kafkaProducer;
        this.brokerLatencyReporter = brokerLatencyReporter;
        this.datacenter = datacenter;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void send(ProducerRecord<K, V> producerRecord,
                     CachedTopic cachedTopic,
                     Message message,
                     Callback callback
    ) {
        HermesTimerContext timer = cachedTopic.startBrokerLatencyTimer();
        Callback meteredCallback = new MeteredCallback(timer, message, cachedTopic, callback);
        producer.send(producerRecord, meteredCallback);
    }

    public List<PartitionInfo> partitionsFor(String topic) {
        return producer.partitionsFor(topic);
    }

    public Map<MetricName, ? extends Metric> metrics() {
        return producer.metrics();
    }

    public void close() {
        producer.close();
    }


    private Supplier<ProduceMetadata> produceMetadataSupplier(RecordMetadata recordMetadata) {
        return () -> {
            String kafkaTopicName = recordMetadata.topic();
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

    private class MeteredCallback implements Callback {

        private final HermesTimerContext hermesTimerContext;
        private final Message message;
        private final CachedTopic cachedTopic;
        private final Callback callback;

        public MeteredCallback(HermesTimerContext hermesTimerContext, Message message, CachedTopic cachedTopic, Callback callback) {
            this.hermesTimerContext = hermesTimerContext;
            this.message = message;
            this.cachedTopic = cachedTopic;
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            callback.onCompletion(metadata, exception);
            Supplier<ProduceMetadata> produceMetadataSupplier = produceMetadataSupplier(metadata);
            brokerLatencyReporter.report(hermesTimerContext, message, cachedTopic.getTopic().getAck(), produceMetadataSupplier);
        }
    }
}
