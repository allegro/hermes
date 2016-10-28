package pl.allegro.tech.hermes.common.message;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Optional;

public class MessageContent {
    private final ContentType contentType;
    private final Optional<CompiledSchema<Schema>> schema;
    private final byte[] content;

    private MessageContent(byte[] content,
                          ContentType contentType,
                          Optional<CompiledSchema<Schema>> schema) {
        this.content = content;
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

    public byte[] getContent() {
        return content;
    }

    public static class Builder {
        private ContentType contentType;
        private Optional<CompiledSchema<Schema>> schema;
        private byte[] content;

        public MessageContent.Builder withContent(byte[] content) {
            this.content = content;
            return this;
        }

        public MessageContent.Builder withSchema(Optional<CompiledSchema<Schema>> schema) {
            this.schema = schema;
            return this;
        }

        public MessageContent.Builder withContentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public MessageContent build() {
            return new MessageContent(content, contentType, schema);
        }
    }
}
