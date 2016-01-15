package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;

import java.util.function.Function;

public class JsonToAvroMigrationKafkaNamesMapper extends NamespaceKafkaNamesMapper {

    public JsonToAvroMigrationKafkaNamesMapper(String namespace) {
        super(namespace);
    }

    public KafkaTopics toKafkaTopics(Topic topic) {
        KafkaTopic primary = mapToKafkaTopic.andThen(appendNamespace).andThen(appendContentTypeSuffix).apply(topic);

        if (topic.wasMigratedFromJsonType()) {
            KafkaTopic secondary = mapToJsonKafkaTopic.andThen(appendNamespace).andThen(appendContentTypeSuffix).apply(topic);
            return new KafkaTopics(primary, secondary);
        }
        return new KafkaTopics(primary);
    }

    private Function<Topic, KafkaTopic> mapToJsonKafkaTopic = it ->
            new KafkaTopic(KafkaTopicName.valueOf(it.getQualifiedName()), ContentType.JSON);

    private Function<KafkaTopic, KafkaTopic> appendContentTypeSuffix = kafkaTopic -> {
        switch (kafkaTopic.contentType()) {
            case JSON:
                return kafkaTopic;
            case AVRO:
                return new KafkaTopic(KafkaTopicName.valueOf(kafkaTopic.name().asString() + "_avro"), kafkaTopic.contentType());
        }

        throw new IllegalStateException("unknown content type '" + kafkaTopic.contentType() + "'");
    };
}
