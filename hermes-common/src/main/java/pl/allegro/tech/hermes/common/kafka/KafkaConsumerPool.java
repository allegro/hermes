package pl.allegro.tech.hermes.common.kafka;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MIN_BYTES_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;


/**
 * This class help us to avoid unnecessarily creating new kafka consumers for the same broker instance, mainly in case of
 * retransmission and migrating topic from json to avro. We map topic and its partitions to specific kafka broker which
 * without caching would lead to creating exactly the same consumer multiple times in short period of time.
 * Additionaly consumers created here are rarely used, so we don't bother about concurrency and thread safety.
 */
public class KafkaConsumerPool {

    private final LoadingCache<Integer, KafkaConsumer<byte[], byte[]>> kafkaConsumers;
    private final BrokerStorage brokerStorage;

    public KafkaConsumerPool(KafkaConsumerPoolConfig poolConfig, BrokerStorage brokerStorage, String configuredBootstrapServers) {
        this.brokerStorage = brokerStorage;
        this.kafkaConsumers = CacheBuilder.newBuilder()
                .expireAfterAccess(poolConfig.getCacheExpirationSeconds(), TimeUnit.SECONDS)
                .removalListener(new KafkaConsumerRemoveListener())
                .build(new KafkaConsumerSupplier(poolConfig, configuredBootstrapServers));
    }

    public KafkaConsumer<byte[], byte[]> get(KafkaTopic topic, int partition) {
        return get(topic.name().asString(), partition);
    }

    public KafkaConsumer<byte[], byte[]> get(String topicName, int partition) {
        try {
            int leaderId = brokerStorage.readLeaderForPartition(new TopicPartition(topicName, partition));
            return kafkaConsumers.get(leaderId);

        } catch (ExecutionException e) {
            String message = String.format("Cannot get KafkaConsumer for topic %s and partition %d", topicName, partition);
            throw new KafkaConsumerPoolException(message, e);
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof BrokerNotFoundForPartitionException) {
                throw (BrokerNotFoundForPartitionException) e.getCause();
            }
            throw e;
        }
    }

    private static class KafkaConsumerSupplier extends CacheLoader<Integer, KafkaConsumer<byte[], byte[]>> {

        private final KafkaConsumerPoolConfig poolConfig;
        private final String configuredBootstrapServers;

        KafkaConsumerSupplier(KafkaConsumerPoolConfig poolConfig, String configuredBootstrapServers) {
            this.poolConfig = poolConfig;
            this.configuredBootstrapServers = configuredBootstrapServers;
        }

        @Override
        public KafkaConsumer<byte[], byte[]> load(Integer leaderId) throws Exception {
            return createKafkaConsumer();
        }

        private KafkaConsumer<byte[], byte[]> createKafkaConsumer() {

            Properties props = new Properties();
            props.put(BOOTSTRAP_SERVERS_CONFIG, configuredBootstrapServers);
            props.put(GROUP_ID_CONFIG, poolConfig.getIdPrefix() + "_" + poolConfig.getConsumerGroupName());
            props.put(RECEIVE_BUFFER_CONFIG, poolConfig.getBufferSizeBytes());
            props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
            props.put(FETCH_MAX_WAIT_MS_CONFIG, poolConfig.getFetchMaxWaitMillis());
            props.put(FETCH_MIN_BYTES_CONFIG, poolConfig.getFetchMinBytes());
            props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

            if (poolConfig.isSaslEnabled()) {
                props.put(SASL_MECHANISM, poolConfig.getSecurityMechanism());
                props.put(SECURITY_PROTOCOL_CONFIG, poolConfig.getSecurityProtocol());
                props.put(SASL_JAAS_CONFIG, poolConfig.getSaslJaasConfig());
            }
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

