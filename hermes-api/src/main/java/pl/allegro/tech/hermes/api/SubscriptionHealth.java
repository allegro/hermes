package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.immutableEnumSet;
import static java.util.Collections.emptySet;

public final class SubscriptionHealth {
    public static final SubscriptionHealth HEALTHY = new SubscriptionHealth(Status.HEALTHY, emptySet());
    public static final SubscriptionHealth NO_DATA = new SubscriptionHealth(Status.NO_DATA, emptySet());

    private final Status status;
    private final ImmutableSet<Problem> problems;

    @JsonCreator
    private SubscriptionHealth(@JsonProperty("status") Status status,
                               @JsonProperty("problems") Set<Problem> problems) {
        this.status = status;
        this.problems = immutableEnumSet(problems);
    }

    public Status getStatus() {
        return status;
    }

    public Set<Problem> getProblems() {
        return problems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionHealth)) return false;
        SubscriptionHealth that = (SubscriptionHealth) o;
        return status == that.status &&
                Objects.equals(problems, that.problems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, problems);
    }

    @Override
    public String toString() {
        return "SubscriptionHealth{" +
                "status=" + status +
                ", problems=" + problems +
                '}';
    }

    public static SubscriptionHealth of(Set<Problem> problems) {
        checkNotNull(problems, "Set of health problems cannot be null");
        if (problems.isEmpty()) {
            return HEALTHY;
        } else {
            return new SubscriptionHealth(Status.UNHEALTHY, problems);
        }
    }

    public enum Status {
        HEALTHY, UNHEALTHY, NO_DATA
    }

    public enum Problem {
        LAGGING, SLOW, UNREACHABLE, TIMING_OUT, MALFUNCTIONING, RECEIVING_MALFORMED_MESSAGES
    }
}