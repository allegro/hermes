package pl.allegro.tech.hermes.common.di.factories;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPoolConfig;

import javax.inject.Inject;

public class SimpleConsumerPoolFactory implements Factory<SimpleConsumerPool> {

    private final ConfigFactory configFactory;

    private final BrokerStorage brokerStorage;

    @Inject
    public SimpleConsumerPoolFactory(ConfigFactory configFactory, BrokerStorage brokerStorage) {
        this.configFactory = configFactory;
        this.brokerStorage = brokerStorage;
    }

    @Override
    public SimpleConsumerPool provide() {
        SimpleConsumerPoolConfig config = new SimpleConsumerPoolConfig(
                configFactory.getIntProperty(Configs.KAFKA_SIMPLE_CONSUMER_CACHE_EXPIRATION_IN_SECONDS),
                configFactory.getIntProperty(Configs.KAFKA_SIMPLE_CONSUMER_TIMEOUT_MS),
                configFactory.getIntProperty(Configs.KAFKA_SIMPLE_CONSUMER_BUFFER_SIZE),
                configFactory.getStringProperty(Configs.KAFKA_SIMPLE_CONSUMER_ID_PREFIX)
        );

        return new SimpleConsumerPool(config, brokerStorage);
    }

    @Override
    public void dispose(SimpleConsumerPool instance) {

    }
}
