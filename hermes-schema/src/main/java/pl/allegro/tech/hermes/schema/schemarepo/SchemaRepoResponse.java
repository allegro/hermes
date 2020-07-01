package pl.allegro.tech.hermes.schema.schemarepo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.SchemaMetadata;

/**
 * SchemaRepo SchemaEntry: https://github.com/schema-repo/schema-repo/blob/master/common/src/main/java/org/schemarepo/SchemaEntry.java
 * In fact SchemaRepo doesn't support value.. So we simulate it by set there id..
 */
class SchemaRepoResponse {

    private final int id;
    private final String schema;

    @JsonCreator
    SchemaRepoResponse(@JsonProperty("id") int id, @JsonProperty("schema") String schema) {
        this.id = id;
        this.schema = schema;
    }

    int getId() {
        return id;
    }

    String getSchema() {
        return schema;
    }

    SchemaMetadata toSchemaMetadata() { return SchemaMetadata.of(schema, id, id); }
}
