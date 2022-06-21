package pl.allegro.tech.hermes.frontend.publishing.handlers;

public class HandlersChainParameters {

    private final boolean keepAliveHeaderEnabled;

    private final int keepAliveHeaderTimeoutSeconds;

    private final boolean authenticationEnabled;

    private final String authenticationMode;

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

    public HandlersChainParameters(boolean keepAliveHeaderEnabled, int keepAliveHeaderTimeoutSeconds, boolean authenticationEnabled, String authenticationMode) {
        this.keepAliveHeaderEnabled = keepAliveHeaderEnabled;
        this.keepAliveHeaderTimeoutSeconds = keepAliveHeaderTimeoutSeconds;
        this.authenticationEnabled = authenticationEnabled;
        this.authenticationMode = authenticationMode;
    }
}
