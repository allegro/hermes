package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.util.Map;

public class MessageContentWrapper {

    private final JsonMessageContentWrapper jsonMessageContentWrapper;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final SchemaRepository<Schema> avroSchemaRepository;

    @Inject
    public MessageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                 AvroMessageContentWrapper avroMessageContentWrapper,
                                 SchemaRepository<Schema> avroSchemaRepository) {
        this.jsonMessageContentWrapper = jsonMessageContentWrapper;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.avroSchemaRepository = avroSchemaRepository;
    }

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic) {
        return unwrap(data, topic, topic.getContentType());
    }

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic, ContentType contentType) {
        if (contentType == ContentType.JSON) {
            return jsonMessageContentWrapper.unwrapContent(data);
        } else if (contentType == ContentType.AVRO) {
            return avroMessageContentWrapper.unwrapContent(data, avroSchemaRepository.getSchema(topic));
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
            return avroMessageContentWrapper.wrapContent(data, id, timestamp, avroSchemaRepository.getSchema(topic), externalMetadata);
        }

        throw new UnsupportedContentTypeException(topic);
    }

}
