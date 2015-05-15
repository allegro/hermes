package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import java.util.Objects;

public class RetentionTime {

    @Min(0)
    private final int duration;

    public RetentionTime(@JsonProperty("duration") int duration) {
        this.duration = duration;
    }

    public static RetentionTime of(int duration) {
        return new RetentionTime(duration);
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RetentionTime other = (RetentionTime) obj;
        return Objects.equals(this.duration, other.duration);
    }
}
