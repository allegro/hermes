package pl.allegro.tech.hermes.frontend.zk;

import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;

public class ZkClientFactory implements Factory<ZkClient> {

    private final ConfigFactory configFactory;

    @Inject
    public ZkClientFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public ZkClient provide() {
        return new ZkClient(configFactory.getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING),
                configFactory.getIntProperty(Configs.ZOOKEEPER_SESSION_TIMEOUT),
                configFactory.getIntProperty(Configs.ZOOKEEPER_CONNECTION_TIMEOUT),
                ZKStringSerializer$.MODULE$);
    }

    @Override
    public void dispose(ZkClient instance) {

    }
}
