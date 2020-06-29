package pl.allegro.tech.hermes.schema.schemarepo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.SchemaWithId;

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

    SchemaWithId toSchemaWithId() {
        return SchemaWithId.valueOf(schema, id);
    }
}
