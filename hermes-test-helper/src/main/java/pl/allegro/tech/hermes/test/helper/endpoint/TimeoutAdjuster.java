package pl.allegro.tech.hermes.test.helper.endpoint;

import java.time.Duration;

import static java.lang.Double.parseDouble;

public class TimeoutAdjuster {
    private static final double timeoutMultiplier = parseDouble(System.getProperty("tests.timeout.multiplier", "1"));

    public static long adjust(long value) {
        return (long) Math.floor(value * timeoutMultiplier);
    }

    public static Duration adjust(Duration duration) {
        return Duration.ofMillis(adjust(duration.toMillis()));
    }
}
