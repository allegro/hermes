package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Component
public class JsonSchemaValidator implements SchemaValidator {

    private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    private final ObjectMapper objectMapper;

    @Autowired
    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void check(String schema) throws InvalidSchemaException {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode schemaNode = objectMapper.readTree(schema);
            ProcessingReport report = jsonSchemaFactory.getSyntaxValidator().validateSchema(schemaNode);

            StreamSupport.stream(report.spliterator(), false).forEach(e -> errors.add(e.getMessage()));
        } catch (IOException e) {
            throw new InvalidSchemaException(e);
        }

        if (!errors.isEmpty()) {
            throw new InvalidSchemaException(errors);
        }
    }

}
