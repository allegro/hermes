package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.schema.SchemaExistenceEnsurer;
import pl.allegro.tech.hermes.schema.SchemaRepository;

public class BasicMessageContentReaderFactory implements MessageContentReaderFactory {

  private final CompositeMessageContentWrapper compositeMessageContentWrapper;
  private final KafkaHeaderExtractor kafkaHeaderExtractor;
  private final SchemaRepository schemaRepository;

  public BasicMessageContentReaderFactory(
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      KafkaHeaderExtractor kafkaHeaderExtractor,
      SchemaRepository schemaRepository) {
    this.compositeMessageContentWrapper = compositeMessageContentWrapper;
    this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    this.schemaRepository = schemaRepository;
  }

  @Override
  public MessageContentReader provide(Topic topic) {
    SchemaExistenceEnsurer schemaExistenceEnsurer = new SchemaExistenceEnsurer(schemaRepository);
    return new BasicMessageContentReader(
        compositeMessageContentWrapper, kafkaHeaderExtractor, topic, schemaExistenceEnsurer);
  }
}
