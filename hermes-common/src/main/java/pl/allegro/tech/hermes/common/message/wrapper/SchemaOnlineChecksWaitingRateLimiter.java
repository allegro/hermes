package pl.allegro.tech.hermes.common.message.wrapper;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.TimeUnit;

public class SchemaOnlineChecksWaitingRateLimiter implements SchemaOnlineChecksRateLimiter {

    private final RateLimiter rateLimiter;

    private final int onlineCheckAcquireWaitMs;

    public SchemaOnlineChecksWaitingRateLimiter(double onlineCheckPermitsPerSeconds, int onlineCheckAcquireWaitMs) {
        this.rateLimiter = RateLimiter.create(onlineCheckPermitsPerSeconds);
        this.onlineCheckAcquireWaitMs = onlineCheckAcquireWaitMs;
    }

    @Override
    public boolean tryAcquireOnlineCheckPermit() {
        return rateLimiter.tryAcquire(onlineCheckAcquireWaitMs, TimeUnit.MILLISECONDS);
    }
}
