package pl.allegro.tech.hermes.domain.topic.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.avro.Schema;

import java.io.IOException;

public interface SchemaCompilersFactory {

    static SchemaCompiler<Schema> avroSchemaCompiler() {
        return source -> new Schema.Parser().parse(source.value());
    }

    static SchemaCompiler<JsonSchema> jsonSchemaCompiler(ObjectMapper objectMapper) {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
        return source -> {
            try {
                return jsonSchemaFactory.getJsonSchema(objectMapper.readTree(source.value()));
            } catch (IOException | ProcessingException e) {
                throw new CouldNotCompileSchemaException(e);
            }
        };
    }

}
