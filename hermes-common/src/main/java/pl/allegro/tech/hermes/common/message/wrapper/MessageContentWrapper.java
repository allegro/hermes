package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.AggregateSchemaRepository;

import javax.inject.Inject;
import java.util.Map;

public class MessageContentWrapper {

    private final JsonMessageContentWrapper jsonMessageContentWrapper;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final AggregateSchemaRepository schemaRepository;

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper,
                                 AggregateSchemaRepository schemaRepository) {
        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.schemaRepository = schemaRepository;
    }

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic) {
        return unwrap(data, topic, topic.getContentType());
    }

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic, ContentType contentType) {
        if (contentType == ContentType.JSON) {
            return jsonMessageContentWrapper.unwrapContent(data);
        } else if (contentType == ContentType.AVRO) {
            return avroMessageContentWrapper.unwrapContent(data, schemaRepository.getAvroSchema(topic).getSchema());
        }

        throw new UnsupportedContentTypeException(topic);
    }

    public byte[] wrap(byte[] data, String id, long timestamp, Topic topic, Map<String, String> externalMetadata) {
        return wrap(data, id, timestamp, topic, topic.getContentType(), externalMetadata);
    }

    public byte[] wrap(byte[] data, String id, long timestamp, Topic topic, ContentType contentType, Map<String, String> externalMetadata) {
        if (contentType == ContentType.JSON) {
            return jsonMessageContentWrapper.wrapContent(data, id, timestamp, externalMetadata);
        } else if (contentType == ContentType.AVRO) {
            return avroMessageContentWrapper.wrapContent(data, id, timestamp, schemaRepository.getAvroSchema(topic).getSchema(), externalMetadata);
        }

        throw new UnsupportedContentTypeException(topic);
    }

}
