package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Constraints {

    private final int consumersNumber;

    @JsonCreator
    public Constraints(@JsonProperty("consumersNumber") int consumersNumber) {
        this.consumersNumber = consumersNumber;
    }

    public int getConsumersNumber() {
        return consumersNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraints that = (Constraints) o;
        return consumersNumber == that.consumersNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumersNumber);
    }
}
