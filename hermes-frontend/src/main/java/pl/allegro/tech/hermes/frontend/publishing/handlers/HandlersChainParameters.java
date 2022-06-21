package pl.allegro.tech.hermes.frontend.publishing.handlers;

public class HandlersChainParameters {

    private final boolean keepAliveHeaderEnabled;

    private final int keepAliveHeaderTimeoutSeconds;

    private final boolean authenticationEnabled;

    private final String authenticationMode;

    private final boolean forceMaxMessageSizePerTopic;

    private final int defaultAsyncTimeout;

    private final int longAsyncTimeout;

    public HandlersChainParameters(boolean keepAliveHeaderEnabled,
                                   int keepAliveHeaderTimeoutSeconds,
                                   boolean authenticationEnabled,
                                   String authenticationMode,
                                   boolean forceMaxMessageSizePerTopic,
                                   int defaultAsyncTimeout,
                                   int longAsyncTimeout) {
        this.keepAliveHeaderEnabled = keepAliveHeaderEnabled;
        this.keepAliveHeaderTimeoutSeconds = keepAliveHeaderTimeoutSeconds;
        this.authenticationEnabled = authenticationEnabled;
        this.authenticationMode = authenticationMode;
        this.forceMaxMessageSizePerTopic = forceMaxMessageSizePerTopic;
        this.defaultAsyncTimeout = defaultAsyncTimeout;
        this.longAsyncTimeout = longAsyncTimeout;
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

    public boolean isForceMaxMessageSizePerTopic() {
        return forceMaxMessageSizePerTopic;
    }

    public int getDefaultAsyncTimeout() {
        return defaultAsyncTimeout;
    }

    public int getLongAsyncTimeout() {
        return longAsyncTimeout;
    }
}
