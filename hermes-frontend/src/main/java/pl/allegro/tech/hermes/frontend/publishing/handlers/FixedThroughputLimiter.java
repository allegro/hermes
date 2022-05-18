package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.codahale.metrics.Metered;
import pl.allegro.tech.hermes.api.TopicName;

import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaViolation;

public class FixedThroughputLimiter implements ThroughputLimiter {
    private final long limit;

    public FixedThroughputLimiter(long limit) {
        this.limit = limit;
    }

    @Override
    public QuotaInsight checkQuota(TopicName topic, Metered throughput) {
        long rate = (long) Math.floor(throughput.getOneMinuteRate());
        return rate > limit ? quotaViolation(rate, limit) : quotaConfirmed();
    }

}
