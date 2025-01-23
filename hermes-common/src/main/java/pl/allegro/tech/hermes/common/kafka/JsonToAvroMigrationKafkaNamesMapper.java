package pl.allegro.tech.hermes.common.kafka;

import java.util.function.Function;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;

public class JsonToAvroMigrationKafkaNamesMapper extends NamespaceKafkaNamesMapper {

  public JsonToAvroMigrationKafkaNamesMapper(String namespace, String namespaceSeparator) {
    super(namespace, namespaceSeparator);
  }

  public KafkaTopics toKafkaTopics(Topic topic) {
    KafkaTopic primary =
        mapToKafkaTopic.andThen(appendNamespace).andThen(appendContentTypeSuffix).apply(topic);

    if (topic.wasMigratedFromJsonType()) {
      KafkaTopic secondary =
          mapToJsonKafkaTopic
              .andThen(appendNamespace)
              .andThen(appendContentTypeSuffix)
              .apply(topic);
      return new KafkaTopics(primary, secondary);
    }
    return new KafkaTopics(primary);
  }

  private final Function<Topic, KafkaTopic> mapToJsonKafkaTopic =
      it -> new KafkaTopic(KafkaTopicName.valueOf(it.getQualifiedName()), ContentType.JSON);

  private final Function<KafkaTopic, KafkaTopic> appendContentTypeSuffix =
      kafkaTopic -> {
        switch (kafkaTopic.contentType()) {
          case JSON:
            return kafkaTopic;
          case AVRO:
            return new KafkaTopic(
                KafkaTopicName.valueOf(kafkaTopic.name().asString() + "_avro"),
                kafkaTopic.contentType());
          default:
            throw new IllegalStateException(
                String.format("Unknown content type '%s'", kafkaTopic.contentType()));
        }
      };
}
