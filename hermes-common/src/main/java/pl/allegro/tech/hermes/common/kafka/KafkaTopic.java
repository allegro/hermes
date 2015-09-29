package pl.allegro.tech.hermes.common.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class KafkaTopic {

    private final String name;

    @JsonCreator
    public KafkaTopic(@JsonProperty("name") String name) {
        this.name = name;
    }

    @JsonGetter(value = "name")
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KafkaTopic that = (KafkaTopic) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("name", name)
                .toString();
    }
}
