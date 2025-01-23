package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.config.TopicProperties;

@Component
public class AvroSchemaValidator implements SchemaValidator {

  private static final Schema HERMES_METADATA_SCHEMA =
      metadataFieldSchema(readAndParseResourceSchema("/avro-schema-metadata-field.avsc"));
  private final boolean metadataFieldIsRequired;

  public AvroSchemaValidator(boolean metadataFieldIsRequired) {
    this.metadataFieldIsRequired = metadataFieldIsRequired;
  }

  @Autowired
  public AvroSchemaValidator(TopicProperties topicProperties) {
    this(topicProperties.isAvroContentTypeMetadataRequired());
  }

  private static Schema metadataFieldSchema(Schema schema) {
    Schema.Field metadata = schema.getField("__metadata");
    if (metadata == null) {
      throw new InvalidSchemaException("Missing Hermes __metadata field");
    }
    return metadata.schema();
  }

  private static Schema readAndParseResourceSchema(String resourceFilePath) {
    try {
      String schema =
          IOUtils.toString(
              AvroSchemaValidator.class.getResourceAsStream(resourceFilePath), "UTF-8");
      return parseSchema(schema);
    } catch (IOException e) {
      throw new RuntimeException("Could not load schema with metadata");
    }
  }

  private static Schema parseSchema(String schema) {
    try {
      return new Schema.Parser().parse(schema);
    } catch (SchemaParseException e) {
      throw new InvalidSchemaException(e);
    }
  }

  @Override
  public void check(String schema) throws InvalidSchemaException {
    checkArgument(!isNullOrEmpty(schema), "Message schema cannot be empty");
    Schema parsedSchema = parseSchema(schema);
    if (metadataFieldIsRequired) {
      checkHermesMetadataField(parsedSchema);
    }
  }

  private void checkHermesMetadataField(Schema parsedSchema) {
    Schema metadata = metadataFieldSchema(parsedSchema);

    boolean metadataTypeMatches = HERMES_METADATA_SCHEMA.getType().equals(metadata.getType());
    boolean metadataNestedTypesMatch =
        HERMES_METADATA_SCHEMA.getTypes().equals(metadata.getTypes());
    boolean valid = metadataTypeMatches && metadataNestedTypesMatch;

    if (!valid) {
      throw new InvalidSchemaException("Invalid types used in field __metadata");
    }
  }
}
