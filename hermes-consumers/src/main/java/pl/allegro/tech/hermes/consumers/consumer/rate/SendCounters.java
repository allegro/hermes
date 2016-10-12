package pl.allegro.tech.hermes.consumers.consumer.rate;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang.math.Fraction;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.math.Fraction.getFraction;

public class SendCounters {

    private final AtomicInteger successes = new AtomicInteger(0);

    private final AtomicInteger failures = new AtomicInteger(0);

    private final AtomicInteger attempted = new AtomicInteger(0);

    private final AtomicDouble rate = new AtomicDouble(0);

    private final Clock clock;

    private volatile long lastReset;

    public SendCounters(Clock clock) {
        this.clock = clock;
    }

    public SendCounters incrementFailures() {
        failures.incrementAndGet();
        return this;
    }

    public SendCounters incrementSuccesses() {
        successes.incrementAndGet();
        return this;
    }

    public SendCounters incrementAttempted() {
        attempted.incrementAndGet();
        return this;
    }

    public void reset() {
        failures.set(0);
        successes.set(0);

        long now = clock.millis();
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(now - lastReset);
        rate.set(attempted.doubleValue() / Math.max(elapsedSeconds, 1));

        attempted.set(0);
        lastReset = now;
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

    public double getRate() {
        return rate.get();
    }

}
