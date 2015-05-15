package pl.allegro.tech.hermes.integration.helper.graphite;

import java.util.Objects;

public class Metric {
    private String name;
    private double value;
    private int timestamp;

    public Metric(String name, double value, int timestamp) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Metric other = (Metric) obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.value, other.value);
    }
}
