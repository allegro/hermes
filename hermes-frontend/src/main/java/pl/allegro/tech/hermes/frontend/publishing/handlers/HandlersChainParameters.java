package pl.allegro.tech.hermes.frontend.publishing.handlers;

import java.time.Duration;

public interface HandlersChainParameters {

    Duration getIdleTimeout();

    Duration getLongIdleTimeout();

    boolean isForceTopicMaxMessageSize();

    boolean isKeepAliveHeaderEnabled();

    Duration getKeepAliveHeaderTimeout();

    boolean isAuthenticationEnabled();

    String getAuthenticationMode();
}
