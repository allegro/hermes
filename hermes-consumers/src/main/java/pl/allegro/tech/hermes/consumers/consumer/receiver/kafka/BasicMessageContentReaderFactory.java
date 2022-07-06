package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Duration;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.consumers.consumer.receiver.SchemaExistenceEnsurer;
import pl.allegro.tech.hermes.schema.SchemaRepository;

public class BasicMessageContentReaderFactory implements MessageContentReaderFactory {

    private final CompositeMessageContentWrapper compositeMessageContentWrapper;
    private final KafkaHeaderExtractor kafkaHeaderExtractor;
    private final SchemaRepository schemaRepository;
    private final ConfigFactory configFactory;

    public BasicMessageContentReaderFactory(CompositeMessageContentWrapper compositeMessageContentWrapper,
                                            KafkaHeaderExtractor kafkaHeaderExtractor, SchemaRepository schemaRepository,
                                            ConfigFactory configFactory) {
        this.compositeMessageContentWrapper = compositeMessageContentWrapper;
        this.kafkaHeaderExtractor = kafkaHeaderExtractor;
        this.schemaRepository = schemaRepository;
        this.configFactory = configFactory;
    }

    @Override
    public MessageContentReader provide(Topic topic) {
        SchemaExistenceEnsurer schemaExistenceEnsurer =
                new SchemaExistenceEnsurer(
                        schemaRepository,
                        Duration.ofMillis(configFactory.getIntProperty(Configs.SCHEMA_EXISTENCE_VALIDATION_INTERVAL_MS))
                );
        return new BasicMessageContentReader(compositeMessageContentWrapper, kafkaHeaderExtractor, topic, schemaExistenceEnsurer);
    }
}
