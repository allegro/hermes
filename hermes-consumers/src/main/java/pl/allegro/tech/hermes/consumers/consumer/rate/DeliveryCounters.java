package pl.allegro.tech.hermes.consumers.consumer.rate;

import org.apache.commons.lang.math.Fraction;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.math.Fraction.getFraction;

public class DeliveryCounters {

    private final AtomicInteger successes = new AtomicInteger(0);

    private final AtomicInteger failures = new AtomicInteger(0);

    public DeliveryCounters incrementFailures() {
        failures.incrementAndGet();
        return this;
    }

    public DeliveryCounters incrementSuccesses() {
        successes.incrementAndGet();
        return this;
    }

    public void reset() {
        failures.set(0);
        successes.set(0);
    }

    public boolean noFailures() {
        return failures.intValue() == 0;
    }

    public boolean hasFailures() {
        return failures.intValue() > 0;
    }

    public boolean majorityOfFailures() {
        return failures.intValue() > successes.intValue();
    }

    public boolean onlySuccessess() {
        return successes.intValue() > 0 && failures.intValue() == 0;
    }

    public boolean failuresRatioExceeds(double threshold) {
        if (hasFailures()) {
            Fraction failuresRatio = getFraction(failures.intValue(), failures.intValue() + successes.intValue());
            return failuresRatio.compareTo(getFraction(threshold)) > 0;
        }
        return false;
    }

}
