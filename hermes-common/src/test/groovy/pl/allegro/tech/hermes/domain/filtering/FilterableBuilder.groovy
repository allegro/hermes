package pl.allegro.tech.hermes.domain.filtering

import org.apache.avro.Schema
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.schema.CompiledSchema

import java.nio.charset.Charset

import static com.google.common.collect.ImmutableMap.of
import static java.nio.charset.StandardCharsets.UTF_8
import static pl.allegro.tech.hermes.api.ContentType.JSON

class FilterableBuilder {
    private ContentType contentType;
    private byte[] content;
    private Map<String, String> externalMetadata;
    private CompiledSchema<Schema> schema

    static Filterable testMessage() {
        return withTestMessage().build()
    }

    FilterableBuilder withContent(String content, Charset charset) {
        this.content = content.getBytes(charset)
        return this
    }

    FilterableBuilder withContent(byte[] content) {
        this.content = content
        return this
    }

    static FilterableBuilder withTestMessage() {
        return new FilterableBuilder()
                .withContent("Some test message", UTF_8)
                .withContentType(JSON)
                .withExternalMetadata(of("Trace-Id", "traceId"))
    }

    FilterableBuilder withSchema(Schema schema, int id, int version) {
        this.schema = CompiledSchema.of(schema, id, version)
        return this
    }

    FilterableBuilder withContentType(ContentType contentType) {
        this.contentType = contentType
        return this
    }


    FilterableBuilder withExternalMetadata(Map<String, String> externalMetadata) {
        this.externalMetadata = externalMetadata
        return this
    }


    Filterable build() {
        return new TestFilterable(content, contentType, schema, externalMetadata)
    }

    class TestFilterable implements Filterable {
        private final byte[] data
        private final ContentType contentType
        private final CompiledSchema<Schema> schema
        private final Map<String, String> externalMetadata

        TestFilterable(byte[] data,
                       ContentType contentType,
                       CompiledSchema<Schema> schema,
                       Map<String, String> externalMetadata) {
            this.data = data
            this.contentType = contentType
            this.schema = schema
            this.externalMetadata = externalMetadata
        }

        @Override
        ContentType getContentType() {
            return contentType
        }

        @Override
        Map<String, String> getExternalMetadata() {
            return externalMetadata
        }

        @Override
        byte[] getData() {
            return data
        }

        @Override
        Optional<CompiledSchema<Schema>> getSchema() {
            return Optional.ofNullable(schema)
        }
    }
}
