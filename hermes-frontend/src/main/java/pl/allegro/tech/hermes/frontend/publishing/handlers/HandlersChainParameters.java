package pl.allegro.tech.hermes.frontend.publishing.handlers;

public class HandlersChainParameters {

    private final int idleTimeout;

    private final int longIdleTimeout;

    private final boolean forceTopicMaxMessageSize;

    private final boolean keepAliveHeaderEnabled;

    private final int keepAliveHeaderTimeoutSeconds;

    private final boolean authenticationEnabled;

    private final String authenticationMode;

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public int getLongIdleTimeout() {
        return longIdleTimeout;
    }

    public boolean isForceTopicMaxMessageSize() {
        return forceTopicMaxMessageSize;
    }

    public boolean isKeepAliveHeaderEnabled() {
        return keepAliveHeaderEnabled;
    }

    public int getKeepAliveHeaderTimeoutSeconds() {
        return keepAliveHeaderTimeoutSeconds;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public String getAuthenticationMode() {
        return authenticationMode;
    }

    public HandlersChainParameters(int idleTimeout,
                                   int longIdleTimeout,
                                   boolean forceTopicMaxMessageSize,
                                   boolean keepAliveHeaderEnabled,
                                   int keepAliveHeaderTimeoutSeconds,
                                   boolean authenticationEnabled,
                                   String authenticationMode) {
        this.idleTimeout = idleTimeout;
        this.longIdleTimeout = longIdleTimeout;
        this.forceTopicMaxMessageSize = forceTopicMaxMessageSize;
        this.keepAliveHeaderEnabled = keepAliveHeaderEnabled;
        this.keepAliveHeaderTimeoutSeconds = keepAliveHeaderTimeoutSeconds;
        this.authenticationEnabled = authenticationEnabled;
        this.authenticationMode = authenticationMode;
    }
}
