package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainParameters;

@ConfigurationProperties(prefix = "frontend.handlers")
public class HandlersChainProperties {

    private int idleTimeout = 65;

    private int longIdleTimeout = 400;

    private boolean forceTopicMaxMessageSize = false;

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getLongIdleTimeout() {
        return longIdleTimeout;
    }

    public void setLongIdleTimeout(int longIdleTimeout) {
        this.longIdleTimeout = longIdleTimeout;
    }

    public boolean isForceTopicMaxMessageSize() {
        return forceTopicMaxMessageSize;
    }

    public void setForceTopicMaxMessageSize(boolean forceTopicMaxMessageSize) {
        this.forceTopicMaxMessageSize = forceTopicMaxMessageSize;
    }

    private AuthenticationProperties authentication = new AuthenticationProperties();

    private KeepAliveHeaderProperties keepAliveHeader = new KeepAliveHeaderProperties();

    public AuthenticationProperties getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthenticationProperties authentication) {
        this.authentication = authentication;
    }

    public KeepAliveHeaderProperties getKeepAliveHeader() {
        return keepAliveHeader;
    }

    public void setKeepAliveHeader(KeepAliveHeaderProperties keepAliveHeader) {
        this.keepAliveHeader = keepAliveHeader;
    }

    public static class AuthenticationProperties {

        private boolean enabled = false;

        private String mode = "constraint_driven";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class KeepAliveHeaderProperties {

        private boolean enabled = false;

        private int timeoutSeconds = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public HandlersChainParameters toHandlersChainParameters() {
        return new HandlersChainParameters(
                this.idleTimeout,
                this.longIdleTimeout,
                this.forceTopicMaxMessageSize,
                this.keepAliveHeader.enabled,
                this.keepAliveHeader.timeoutSeconds,
                this.authentication.enabled,
                this.authentication.mode
        );
    }
}
