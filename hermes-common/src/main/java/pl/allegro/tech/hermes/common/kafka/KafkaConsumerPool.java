package pl.allegro.tech.hermes.common.kafka;

import com.google.common.cache.*;
import java.util.Properties;
import kafka.common.TopicAndPartition;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import pl.allegro.tech.hermes.common.broker.BrokerDetails;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG;

public class KafkaConsumerPool {

    private final LoadingCache<Integer, KafkaConsumer<byte[], byte[]>> kafkaConsumers;
    private final BrokerStorage brokerStorage;

    public KafkaConsumerPool(KafkaConsumerPoolConfig poolConfig, BrokerStorage brokerStorage) {
        this.brokerStorage = brokerStorage;
        this.kafkaConsumers = CacheBuilder.newBuilder()
                .expireAfterAccess(poolConfig.getCacheExpiration(), TimeUnit.SECONDS)
                .removalListener(new KafkaConsumerRemoveListener())
                .build(new KafkaConsumerSupplier(brokerStorage, poolConfig));
    }

    public KafkaConsumer<byte[], byte[]> get(Integer leaderId) {
        try {
            return kafkaConsumers.get(leaderId);
        } catch (ExecutionException e) {
            throw new KafkaConsumerPoolException(String.format("Cannot get KafkaConsumer for leader %d", leaderId), e);
        }
    }

    public KafkaConsumer<byte[], byte[]> get(KafkaTopic topic, int partition) {
        return get(brokerStorage.readLeaderForPartition(new TopicAndPartition(topic.name().asString(), partition)));
    }

    private static class KafkaConsumerSupplier extends CacheLoader<Integer, KafkaConsumer<byte[], byte[]>> {
        private BrokerStorage brokerStorage;

        private final KafkaConsumerPoolConfig poolConfig;

        public KafkaConsumerSupplier(BrokerStorage brokerStorage, KafkaConsumerPoolConfig poolConfig) {
            this.brokerStorage = brokerStorage;
            this.poolConfig = poolConfig;
        }

        @Override
        public KafkaConsumer<byte[], byte[]> load(Integer brokerId) throws Exception {
            return createKafkaConsumer(brokerId);
        }

        private KafkaConsumer<byte[], byte[]> createKafkaConsumer(Integer brokerId) {
            BrokerDetails brokerDetails = brokerStorage.readBrokerDetails(brokerId);
            Properties props = new Properties();
            props.put(BOOTSTRAP_SERVERS_CONFIG, brokerDetails.getHost() + ":" + brokerDetails.getPort());
            props.put(GROUP_ID_CONFIG, poolConfig.getIdPrefix() + "_" + poolConfig.getConsumerGroupName());
            props.put(RECEIVE_BUFFER_CONFIG, poolConfig.getBufferSize());
            props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            return new KafkaConsumer<>(props);
        }
    }

    private static class KafkaConsumerRemoveListener implements RemovalListener<Integer, KafkaConsumer<byte[], byte[]>> {
        @Override
        public void onRemoval(RemovalNotification<Integer, KafkaConsumer<byte[], byte[]>> notification) {
            notification.getValue().close();
        }
    }


}

