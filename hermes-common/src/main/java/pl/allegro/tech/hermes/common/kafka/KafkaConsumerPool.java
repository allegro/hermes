package pl.allegro.tech.hermes.common.kafka;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Properties;
import kafka.common.TopicAndPartition;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.common.broker.BrokerDetails;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MIN_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG;

public class KafkaConsumerPool {

    private final LoadingCache<TopicPartition, KafkaConsumer<byte[], byte[]>> kafkaConsumers;

    public KafkaConsumerPool(KafkaConsumerPoolConfig poolConfig, BrokerStorage brokerStorage) {
        this.kafkaConsumers = CacheBuilder.newBuilder()
                .expireAfterAccess(poolConfig.getCacheExpiration(), TimeUnit.SECONDS)
                .removalListener(new KafkaConsumerRemoveListener())
                .build(new KafkaConsumerSupplier(brokerStorage, poolConfig));
    }

    public KafkaConsumer<byte[], byte[]> get(KafkaTopic topic, int partition) {
        try {
            return kafkaConsumers.get(new TopicPartition(topic.name().asString(), partition));
        } catch (ExecutionException e) {
            String message = String.format("Cannot get KafkaConsumer for topic %s and partition %d", topic.name().asString(), partition);
            throw new KafkaConsumerPoolException(message, e);
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof BrokerNotFoundForPartitionException) {
                throw (BrokerNotFoundForPartitionException)e.getCause();
            }
            throw e;
        }
    }

    private static class KafkaConsumerSupplier extends CacheLoader<TopicPartition, KafkaConsumer<byte[], byte[]>> {
        private BrokerStorage brokerStorage;

        private final KafkaConsumerPoolConfig poolConfig;

        KafkaConsumerSupplier(BrokerStorage brokerStorage, KafkaConsumerPoolConfig poolConfig) {
            this.brokerStorage = brokerStorage;
            this.poolConfig = poolConfig;
        }

        @Override
        public KafkaConsumer<byte[], byte[]> load(TopicPartition topicPartition) throws Exception {
            return createKafkaConsumer(topicPartition);
        }

        private KafkaConsumer<byte[], byte[]> createKafkaConsumer(TopicPartition topicPartition) {
            int brokerId = brokerStorage.readLeaderForPartition(new TopicAndPartition(topicPartition));
            BrokerDetails brokerDetails = brokerStorage.readBrokerDetails(brokerId);

            Properties props = new Properties();
            props.put(BOOTSTRAP_SERVERS_CONFIG, brokerDetails.getHost() + ":" + brokerDetails.getPort());
            props.put(GROUP_ID_CONFIG, poolConfig.getIdPrefix() + "_" + poolConfig.getConsumerGroupName());
            props.put(RECEIVE_BUFFER_CONFIG, poolConfig.getBufferSize());
            props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
            props.put(FETCH_MAX_WAIT_MS_CONFIG, poolConfig.getFetchMaxWaitMillis());
            props.put(FETCH_MIN_BYTES_CONFIG, poolConfig.getFetchMinBytes());
            props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            return new KafkaConsumer<>(props);
        }
    }

    private static class KafkaConsumerRemoveListener implements RemovalListener<TopicPartition, KafkaConsumer<byte[], byte[]>> {
        @Override
        public void onRemoval(RemovalNotification<TopicPartition, KafkaConsumer<byte[], byte[]>> notification) {
            notification.getValue().close();
        }
    }


}

