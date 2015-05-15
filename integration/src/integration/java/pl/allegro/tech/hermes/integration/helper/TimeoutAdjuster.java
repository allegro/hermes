package pl.allegro.tech.hermes.integration.helper;

import com.jayway.awaitility.Duration;

import java.util.concurrent.TimeUnit;

import static java.lang.Double.parseDouble;

public class TimeoutAdjuster {
    private static double timeoutMultiplier = parseDouble(System.getProperty("tests.timeout.multiplier", "1"));

    public static long adjust(long value) {
        return (long) Math.floor(value * timeoutMultiplier);
    }

    public static Duration adjust(Duration duration) {
        return new Duration(adjust(duration.getValueInMS()), TimeUnit.MILLISECONDS);
    }
}
