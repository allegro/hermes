package pl.allegro.tech.hermes.common.kafka;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;

public class KafkaNamesMapperFactory implements Factory<KafkaNamesMapper> {

    private final String namespace;

    @Inject
    public KafkaNamesMapperFactory(ConfigFactory configFactory) {
        this.namespace = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE);
    }

    @Override
    public KafkaNamesMapper provide() {
        return new KafkaNamesMapperHolder(new JsonToAvroKafkaNamesMapper(namespace));
    }

    @Override
    public void dispose(KafkaNamesMapper instance) {

    }
}
