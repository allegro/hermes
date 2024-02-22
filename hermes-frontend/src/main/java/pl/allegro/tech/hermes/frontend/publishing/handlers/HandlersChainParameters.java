package pl.allegro.tech.hermes.frontend.publishing.handlers;

import java.time.Duration;

public interface HandlersChainParameters {

    Duration getIdleTimeout();

    Duration getLongIdleTimeout();

    Duration getGlobalAsyncTimeout();

    boolean isForceTopicMaxMessageSize();

    boolean isKeepAliveHeaderEnabled();

    Duration getKeepAliveHeaderTimeout();

    boolean isAuthenticationEnabled();

    String getAuthenticationMode();
}
