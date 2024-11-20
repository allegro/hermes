package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public class Constraints {

  @Min(1)
  private final int consumersNumber;

  @Size(max = 1024)
  private final String reason;

  @JsonCreator
  public Constraints(
      @JsonProperty("consumersNumber") int consumersNumber, @JsonProperty("reason") String reason) {
    this.consumersNumber = consumersNumber;
    this.reason = reason;
  }

  public int getConsumersNumber() {
    return consumersNumber;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Constraints that = (Constraints) o;
    return consumersNumber == that.consumersNumber && Objects.equals(reason, that.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consumersNumber, reason);
  }
}
