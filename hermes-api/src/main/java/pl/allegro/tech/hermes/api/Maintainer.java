package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public final class Maintainer {

    @NotNull
    private final String source;

    @NotNull
    private final String id;

    @JsonCreator
    public Maintainer(@JsonProperty("source") String source,
                      @JsonProperty("id") String id) {
        this.source = source;
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Maintainer that = (Maintainer) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id);
    }

    @Override
    public String toString() {
        return "Maintainer{" +
                "source='" + source + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
