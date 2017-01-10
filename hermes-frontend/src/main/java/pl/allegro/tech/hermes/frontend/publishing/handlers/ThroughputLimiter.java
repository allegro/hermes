package pl.allegro.tech.hermes.frontend.publishing.handlers;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import static java.lang.String.format;

public class ThroughputLimiter {
    private long limit;

    public ThroughputLimiter(long limit) {
        this.limit = limit;
    }

    public void check(CachedTopic cachedTopic) {
        long current = (long) Math.floor(cachedTopic.getThroughput());
        if (current > limit) {
            throw new QuotaViolationException(current, limit);
        }
    }

    public static final class QuotaViolationException extends RuntimeException {
        QuotaViolationException(long current, long limit) {
            super(format("Current throughput exceeded limit [current:%s, limit:%s].",
                    current, limit));
        }
    }
}
