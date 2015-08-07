package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@Component
public class AvroSchemaValidator implements SchemaValidator {

    @Override
    public void check(String schema) throws InvalidSchemaException {
        checkArgument(!isNullOrEmpty(schema), "Message schema cannot be empty");

        try {
            new Schema.Parser().parse(schema);
        } catch (SchemaParseException e) {
            throw new InvalidSchemaException(e);
        }
    }

}
