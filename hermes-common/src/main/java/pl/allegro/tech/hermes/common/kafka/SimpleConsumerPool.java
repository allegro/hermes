package pl.allegro.tech.hermes.common.kafka;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import kafka.common.TopicAndPartition;
import kafka.javaapi.consumer.SimpleConsumer;
import pl.allegro.tech.hermes.common.broker.BrokerDetails;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SimpleConsumerPool {

    private final LoadingCache<Integer, SimpleConsumer> simpleConsumers;
    private final BrokerStorage brokerStorage;
    private final int bufferSize;

    public SimpleConsumerPool(SimpleConsumerPoolConfig poolConfig, BrokerStorage brokerStorage) {
        this.brokerStorage = brokerStorage;
        this.bufferSize = poolConfig.getBufferSize();
        this.simpleConsumers = CacheBuilder.newBuilder()
                .expireAfterAccess(poolConfig.getCacheExpiration(), TimeUnit.SECONDS)
                .removalListener(new SimpleConsumerRemoveListener())
                .build(new SimpleConsumerSupplier(brokerStorage, poolConfig));
    }

    public SimpleConsumer get(Integer leaderId) {
        try {
            return simpleConsumers.get(leaderId);
        } catch (ExecutionException e) {
            throw new SimpleConsumerPoolException(String.format("Cannot get SimpleConsumer for leader %d", leaderId), e);
        }
    }

    public SimpleConsumer get(String topic, int partition) {
        return get(brokerStorage.readLeaderForPartition(new TopicAndPartition(topic, partition)));
    }

    public int getBufferSize() {
        return bufferSize;
    }

    private static class SimpleConsumerSupplier extends CacheLoader<Integer, SimpleConsumer> {
        private BrokerStorage brokerStorage;

        private final int timeout;
        private final int bufferSize;
        private final String namePrefix;

        public SimpleConsumerSupplier(BrokerStorage brokerStorage, SimpleConsumerPoolConfig poolConfig) {
            this.brokerStorage = brokerStorage;

            timeout = poolConfig.getTimeout();
            bufferSize = poolConfig.getBufferSize();
            namePrefix = poolConfig.getIdPrefix() + UUID.randomUUID().toString();
        }

        @Override
        public SimpleConsumer load(Integer brokerId) throws Exception {
            BrokerDetails brokerDetails = brokerStorage.readBrokerDetails(brokerId);
            return new SimpleConsumer(brokerDetails.getHost(), brokerDetails.getPort(), timeout, bufferSize, namePrefix + brokerId);
        }
    }

    private static class SimpleConsumerRemoveListener implements RemovalListener<Integer, SimpleConsumer> {
        @Override
        public void onRemoval(RemovalNotification<Integer, SimpleConsumer> notification) {
            notification.getValue().close();
        }
    }


}

