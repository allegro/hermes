package pl.allegro.tech.hermes.common.message.wrapper;

public interface SchemaOnlineChecksRateLimiter {

    boolean tryAcquireOnlineCheckPermit();
}
