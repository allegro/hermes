package pl.allegro.tech.hermes.common.message.wrapper;

import com.google.common.util.concurrent.RateLimiter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class SchemaOnlineChecksWaitingRateLimiter implements SchemaOnlineChecksRateLimiter {

    private final RateLimiter rateLimiter;

    private final Duration onlineCheckAcquireWait;

    public SchemaOnlineChecksWaitingRateLimiter(double onlineCheckPermitsPerSeconds, Duration onlineCheckAcquireWait) {
        this.rateLimiter = RateLimiter.create(onlineCheckPermitsPerSeconds);
        this.onlineCheckAcquireWait = onlineCheckAcquireWait;
    }

    @Override
    public boolean tryAcquireOnlineCheckPermit() {
        return rateLimiter.tryAcquire(onlineCheckAcquireWait.toMillis(), TimeUnit.MILLISECONDS);
    }
}
