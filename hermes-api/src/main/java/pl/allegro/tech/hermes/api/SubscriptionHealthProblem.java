package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.LAGGING;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.MALFUNCTIONING;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.RECEIVING_MALFORMED_MESSAGES;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.TIMING_OUT;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.ProblemCode.UNREACHABLE;

public class SubscriptionHealthProblem {

    public enum ProblemCode {
        LAGGING, UNREACHABLE, TIMING_OUT, MALFUNCTIONING, RECEIVING_MALFORMED_MESSAGES
    }

    private final ProblemCode code;
    private final String description;

    @JsonCreator
    private SubscriptionHealthProblem(@JsonProperty("code") ProblemCode code,
                                      @JsonProperty("description") String description) {
        this.code = code;
        this.description = description;
    }

    public static SubscriptionHealthProblem lagging(long subscriptionLag) {
        return new SubscriptionHealthProblem(
                LAGGING,
                format("Subscription lag is growing, current value is %d messages", subscriptionLag)
        );
    }

    public static SubscriptionHealthProblem malfunctioning(double code5xxErrorsRate) {
        return new SubscriptionHealthProblem(
                MALFUNCTIONING,
                format("Consuming service returns a lot of 5xx codes, currently %.0f 5xx/s", code5xxErrorsRate)
        );
    }

    public static SubscriptionHealthProblem receivingMalformedMessages(double code4xxErrorsRate) {
        return new SubscriptionHealthProblem(
                RECEIVING_MALFORMED_MESSAGES,
                format("Consuming service returns a lot of 4xx codes, currently %.0f 4xx/s", code4xxErrorsRate)
        );
    }

    public static SubscriptionHealthProblem timingOut(double timeoutsRate) {
        return new SubscriptionHealthProblem(
                TIMING_OUT,
                format("Consuming service times out a lot, currently %.0f timeouts/s", timeoutsRate)
        );
    }

    public static SubscriptionHealthProblem unreachable(double otherErrorsRate) {
        return new SubscriptionHealthProblem(
                UNREACHABLE,
                format("Unable to connect to consuming service instances, current rate is %.0f failures/s", otherErrorsRate)
        );
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
