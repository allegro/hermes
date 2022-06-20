package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainParameters;

@ConfigurationProperties(prefix = "frontend")
public class HandlersChainProperties {

    private boolean keepAliveHeaderEnabled = false;

    private int keepAliveHeaderTimeoutSeconds = 1;

    private boolean authenticationEnabled = false;

    private String authenticationMode = "constraint_driven";

    public boolean isKeepAliveHeaderEnabled() {
        return keepAliveHeaderEnabled;
    }

    public void setKeepAliveHeaderEnabled(boolean keepAliveHeaderEnabled) {
        this.keepAliveHeaderEnabled = keepAliveHeaderEnabled;
    }

    public int getKeepAliveHeaderTimeoutSeconds() {
        return keepAliveHeaderTimeoutSeconds;
    }

    public void setKeepAliveHeaderTimeoutSeconds(int keepAliveHeaderTimeoutSeconds) {
        this.keepAliveHeaderTimeoutSeconds = keepAliveHeaderTimeoutSeconds;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public void setAuthenticationEnabled(boolean authenticationEnabled) {
        this.authenticationEnabled = authenticationEnabled;
    }

    public String getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(String authenticationMode) {
        this.authenticationMode = authenticationMode;
    }

    public HandlersChainParameters toHandlersChainParameters() {
        return new HandlersChainParameters(
                this.keepAliveHeaderEnabled,
                this.keepAliveHeaderTimeoutSeconds,
                this.authenticationEnabled,
                this.authenticationMode
        );
    }
}
