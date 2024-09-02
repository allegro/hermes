package pl.allegro.tech.hermes.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;

public final class SubscriptionHealth {
  public static final SubscriptionHealth HEALTHY =
      new SubscriptionHealth(Status.HEALTHY, emptySet());
  public static final SubscriptionHealth NO_DATA =
      new SubscriptionHealth(Status.NO_DATA, emptySet());

  private final Status status;
  private final ImmutableSet<SubscriptionHealthProblem> problems;

  @JsonCreator
  private SubscriptionHealth(
      @JsonProperty("status") Status status,
      @JsonProperty("problems") Set<SubscriptionHealthProblem> problems) {
    this.status = status;
    this.problems = ImmutableSet.copyOf(problems);
  }

  public Status getStatus() {
    return status;
  }

  public Set<SubscriptionHealthProblem> getProblems() {
    return problems;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SubscriptionHealth)) {
      return false;
    }
    SubscriptionHealth that = (SubscriptionHealth) o;
    return status == that.status && Objects.equals(problems, that.problems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, problems);
  }

  @Override
  public String toString() {
    return "SubscriptionHealth{" + "status=" + status + ", problems=" + problems + '}';
  }

  public static SubscriptionHealth of(Set<SubscriptionHealthProblem> problems) {
    checkNotNull(problems, "Set of health problems cannot be null");
    if (problems.isEmpty()) {
      return HEALTHY;
    } else {
      return new SubscriptionHealth(Status.UNHEALTHY, problems);
    }
  }

  public enum Status {
    HEALTHY,
    UNHEALTHY,
    NO_DATA
  }
}
