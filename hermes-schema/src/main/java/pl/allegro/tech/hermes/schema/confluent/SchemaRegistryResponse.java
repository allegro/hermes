package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

class SchemaRegistryResponse {

    @NotNull
    private final String subject;

    @NotNull
    private final Integer id;

    @NotNull
    private final Integer version;

    @NotNull
    private final String schema;

    @JsonCreator
    SchemaRegistryResponse(@JsonProperty("subject") String subject,
                                  @JsonProperty("id") Integer id,
                                  @JsonProperty("version") Integer version,
                                  @JsonProperty("schema") String schema) {
        this.subject = subject;
        this.version = version;
        this.id = id;
        this.schema = schema;
    }

    public String getSubject() {
        return subject;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getId() {
        return id;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaRegistryResponse that = (SchemaRegistryResponse) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(version, that.version) &&
                Objects.equals(id, that.id) &&
                Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, version, id, schema);
    }
}
