package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;

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

    public UnwrappedMessageContent unwrap(byte[] data, Topic topic, Topic.ContentType contentType) {
        if (contentType == Topic.ContentType.JSON) {
            return jsonMessageContentWrapper.unwrapContent(data);
        } else if (contentType == Topic.ContentType.AVRO) {
            return avroMessageContentWrapper.unwrapContent(data, avroSchemaRepository.getSchema(topic));
        }

        throw new UnsupportedContentTypeException(topic);
    }

    public byte[] wrap(byte[] data, String id, long timestamp, Topic topic) {
        if (topic.getContentType() == Topic.ContentType.JSON) {
            return jsonMessageContentWrapper.wrapContent(data, id, timestamp);
        } else if (topic.getContentType() == Topic.ContentType.AVRO) {
            return avroMessageContentWrapper.wrapContent(data, id, timestamp, avroSchemaRepository.getSchema(topic));
        }

        throw new UnsupportedContentTypeException(topic);
    }

}
