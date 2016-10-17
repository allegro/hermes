package pl.allegro.tech.hermes.common.message;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Optional;

public class MessageContent {
    private ContentType contentType;
    private Optional<CompiledSchema<Schema>> schema;
    private byte[] data;


    private MessageContent() {
    }

    public MessageContent(byte[] content,
                          ContentType contentType,
                          Optional<CompiledSchema<Schema>> schema) {
        this.data = content;
        this.contentType = contentType;
        this.schema = schema;
    }

    @SuppressWarnings("unchecked")
    public Optional<CompiledSchema<Schema>> getSchema() {
        return schema;
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

        public MessageContent.Builder withSchema(Optional<CompiledSchema<Schema>> schema) {
            this.message.schema = schema;
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
