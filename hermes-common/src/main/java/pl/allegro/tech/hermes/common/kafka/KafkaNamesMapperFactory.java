package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

public class KafkaNamesMapperFactory {

    private final String namespace;
    private final String namespaceSeparator;

    public KafkaNamesMapperFactory(ConfigFactory configFactory) {
        this.namespace = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE);
        this.namespaceSeparator = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE_SEPARATOR);
    }

    public KafkaNamesMapper provide() {
        return new NamespaceKafkaNamesMapper(namespace, namespaceSeparator);
    }
}
