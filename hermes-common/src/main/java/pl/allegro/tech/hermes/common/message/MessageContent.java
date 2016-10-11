package pl.allegro.tech.hermes.common.message;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;

import java.util.Optional;

public class MessageContent {
    private ContentType contentType;
    private Optional<CompiledSchema<Object>> schema;
    private byte[] data;


    private MessageContent() {
    }

    public MessageContent(byte[] content,
                          ContentType contentType,
                          Optional<CompiledSchema<Object>> schema) {
        this.data = content;
        this.contentType = contentType;
        this.schema = schema;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<CompiledSchema<T>> getSchema() {
        return schema.map(schema -> (CompiledSchema<T>) schema);
    }

    public ContentType getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public static class Builder {
        private final MessageContent message;

        public Builder() {
            message = new MessageContent();
        }

        public MessageContent.Builder withData(byte[] data) {
            this.message.data = data;
            return this;
        }

        public MessageContent.Builder withSchema(CompiledSchema<Object> schema) {
            this.message.schema = Optional.of(schema);
            return this;
        }

        public MessageContent.Builder withContentType(ContentType contentType) {
            this.message.contentType = contentType;
            return this;
        }

        public MessageContent build() {
            return message;
        }
    }
}
