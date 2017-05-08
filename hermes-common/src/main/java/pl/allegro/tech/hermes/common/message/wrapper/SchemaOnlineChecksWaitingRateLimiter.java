package pl.allegro.tech.hermes.common.message.wrapper;

import com.google.common.util.concurrent.RateLimiter;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SchemaOnlineChecksWaitingRateLimiter implements SchemaOnlineChecksRateLimiter {

    private final RateLimiter rateLimiter;

    private final int onlineCheckAcquireWaitMs;

    @Inject
    public SchemaOnlineChecksWaitingRateLimiter(ConfigFactory configFactory) {
        double onlineCheckPermitsPerSeconds = configFactory.getDoubleProperty(Configs.SCHEMA_REPOSITORY_ONLINE_CHECK_PERMITS_PER_SECOND);
        this.rateLimiter = RateLimiter.create(onlineCheckPermitsPerSeconds);
        this.onlineCheckAcquireWaitMs = configFactory.getIntProperty(Configs.SCHEMA_REPOSITORY_ONLINE_CHECK_ACQUIRE_WAIT_MS);
    }

    @Override
    public boolean tryAcquireOnlineCheckPermit() {
        return rateLimiter.tryAcquire(onlineCheckAcquireWaitMs, TimeUnit.MILLISECONDS);
    }
}
