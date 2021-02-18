package pl.allegro.tech.hermes.common.kafka;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;

public class KafkaNamesMapperFactory implements Factory<KafkaNamesMapper> {

    private final String namespace;
    private final String namespaceSeparator;

    @Inject
    public KafkaNamesMapperFactory(ConfigFactory configFactory) {
        this.namespace = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE);
        this.namespaceSeparator = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE_SEPARATOR);
    }

    @Override
    public KafkaNamesMapper provide() {
        return new NamespaceKafkaNamesMapper(namespace, namespaceSeparator);
    }

    @Override
    public void dispose(KafkaNamesMapper instance) {

    }
}
