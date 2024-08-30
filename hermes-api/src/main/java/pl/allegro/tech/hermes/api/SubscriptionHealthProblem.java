package pl.allegro.tech.hermes.api;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.LAGGING;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.MALFUNCTIONING;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.RECEIVING_MALFORMED_MESSAGES;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.TIMING_OUT;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.UNREACHABLE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class SubscriptionHealthProblem {

  public enum ProblemCode {
    LAGGING,
    UNREACHABLE,
    TIMING_OUT,
    MALFUNCTIONING,
    RECEIVING_MALFORMED_MESSAGES
  }

  private final ProblemCode code;
  private final String description;

  @JsonCreator
  private SubscriptionHealthProblem(
      @JsonProperty("code") ProblemCode code, @JsonProperty("description") String description) {
    this.code = code;
    this.description = description;
  }

  public static SubscriptionHealthProblem lagging(long subscriptionLag, String subscriptionName) {
    return new SubscriptionHealthProblem(
        LAGGING,
        format(
            "Lag is growing on subscription %s, current value is %d messages",
            subscriptionName, subscriptionLag));
  }

  public static SubscriptionHealthProblem malfunctioning(
      double code5xxErrorsRate, String subscriptionName) {
    return new SubscriptionHealthProblem(
        MALFUNCTIONING,
        format(
            "Consuming service returns a lot of 5xx codes for subscription %s, currently %.0f 5xx/s",
            subscriptionName, code5xxErrorsRate));
  }

  public static SubscriptionHealthProblem receivingMalformedMessages(
      double code4xxErrorsRate, String subscriptionName) {
    return new SubscriptionHealthProblem(
        RECEIVING_MALFORMED_MESSAGES,
        format(
            "Consuming service returns a lot of 4xx codes for subscription %s, currently %.0f 4xx/s",
            subscriptionName, code4xxErrorsRate));
  }

  public static SubscriptionHealthProblem timingOut(double timeoutsRate, String subscriptionName) {
    return new SubscriptionHealthProblem(
        TIMING_OUT,
        format(
            "Consuming service times out a lot for subscription %s, currently %.0f timeouts/s",
            subscriptionName, timeoutsRate));
  }

  public static SubscriptionHealthProblem unreachable(
      double otherErrorsRate, String subscriptionName) {
    return new SubscriptionHealthProblem(
        UNREACHABLE,
        format(
            "Unable to connect to consuming service instances for subscription %s, current rate is %.0f failures/s",
            subscriptionName, otherErrorsRate));
  }

  public ProblemCode getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return code.name();
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, description);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SubscriptionHealthProblem other = (SubscriptionHealthProblem) obj;
    return Objects.equals(this.code, other.code)
        && Objects.equals(this.description, other.description);
  }
}
