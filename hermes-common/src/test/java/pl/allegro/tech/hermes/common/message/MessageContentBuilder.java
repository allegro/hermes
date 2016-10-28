package pl.allegro.tech.hermes.common.message;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static pl.allegro.tech.hermes.common.message.MessageContent.Builder;

public final class MessageContentBuilder {
    public static final String TEST_MESSAGE_CONTENT = "Some test message";

    private ContentType contentType;
    private byte[] content;
    private Optional<CompiledSchema<Schema>> schema = Optional.empty();

    private MessageContentBuilder() {
    }

    public static MessageContent testMessage() {
        return MessageContentBuilder.withTestMessage().build();
    }

    public static MessageContentBuilder withTestMessage() {
        return new MessageContentBuilder()
                .withContent(TEST_MESSAGE_CONTENT, StandardCharsets.UTF_8)
                .withContentType(ContentType.JSON);
    }

    public MessageContent build() {
        return new Builder().withContentType(contentType)
                .withContent(content)
                .withSchema(schema)
                .build();
    }

    public MessageContentBuilder withSchema(Schema schema, int version) {
        this.schema = Optional.of(new CompiledSchema<>(schema, SchemaVersion.valueOf(version)));
        return this;
    }

    public MessageContentBuilder withContent(String content, Charset charset) {
        this.content = content.getBytes(charset);
        return this;
    }

    public MessageContentBuilder withContent(byte[] content) {
        this.content = content;
        return this;
    }

    public MessageContentBuilder withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }
}
