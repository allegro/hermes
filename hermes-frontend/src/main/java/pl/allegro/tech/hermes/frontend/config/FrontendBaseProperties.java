package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainParameters;

@ConfigurationProperties(prefix = "frontend")
public class FrontendBaseProperties {

    private int port = 8080;
    private String host = "0.0.0.0";
    private int idleTimeout = 65;
    private int longIdleTimeout = 400;
    private int readTimeout = 2000;
    private int requestParseTimeout = 5000;
    private int maxHeaders = 20;
    private int maxParameters = 10;
    private int maxCookies = 10;
    private int backlogSize = 10000;
    private int ioThreadsCount = Runtime.getRuntime().availableProcessors() * 2;
    private int workerThreadCount = 200;
    private boolean alwaysKeepAlive = false;
    private boolean keepAlive = false;
    private boolean requestDumper = false;
    private int bufferSize = 16384;
    private boolean gracefulShutdownEnabled = true;
    private int gracefulShutdownInitialWaitMs = 10000;
    private boolean http2Enabled = false;
    private boolean forceTopicMaxMessageSize = false;
    private AuthenticationProperties authentication = new AuthenticationProperties();
    private KeepAliveHeaderProperties keepAliveHeader = new KeepAliveHeaderProperties();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

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

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRequestParseTimeout() {
        return requestParseTimeout;
    }

    public void setRequestParseTimeout(int requestParseTimeout) {
        this.requestParseTimeout = requestParseTimeout;
    }

    public int getMaxHeaders() {
        return maxHeaders;
    }

    public void setMaxHeaders(int maxHeaders) {
        this.maxHeaders = maxHeaders;
    }

    public int getMaxParameters() {
        return maxParameters;
    }

    public void setMaxParameters(int maxParameters) {
        this.maxParameters = maxParameters;
    }

    public int getMaxCookies() {
        return maxCookies;
    }

    public void setMaxCookies(int maxCookies) {
        this.maxCookies = maxCookies;
    }

    public int getBacklogSize() {
        return backlogSize;
    }

    public void setBacklogSize(int backlogSize) {
        this.backlogSize = backlogSize;
    }

    public int getIoThreadsCount() {
        return ioThreadsCount;
    }

    public void setIoThreadsCount(int ioThreadsCount) {
        this.ioThreadsCount = ioThreadsCount;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount(int workerThreadCount) {
        this.workerThreadCount = workerThreadCount;
    }

    public boolean isAlwaysKeepAlive() {
        return alwaysKeepAlive;
    }

    public void setAlwaysKeepAlive(boolean alwaysKeepAlive) {
        this.alwaysKeepAlive = alwaysKeepAlive;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isRequestDumper() {
        return requestDumper;
    }

    public void setRequestDumper(boolean requestDumper) {
        this.requestDumper = requestDumper;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isGracefulShutdownEnabled() {
        return gracefulShutdownEnabled;
    }

    public void setGracefulShutdownEnabled(boolean gracefulShutdownEnabled) {
        this.gracefulShutdownEnabled = gracefulShutdownEnabled;
    }

    public int getGracefulShutdownInitialWaitMs() {
        return gracefulShutdownInitialWaitMs;
    }

    public void setGracefulShutdownInitialWaitMs(int gracefulShutdownInitialWaitMs) {
        this.gracefulShutdownInitialWaitMs = gracefulShutdownInitialWaitMs;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public void setHttp2Enabled(boolean http2Enabled) {
        this.http2Enabled = http2Enabled;
    }

    public boolean isForceTopicMaxMessageSize() {
        return forceTopicMaxMessageSize;
    }

    public void setForceTopicMaxMessageSize(boolean forceTopicMaxMessageSize) {
        this.forceTopicMaxMessageSize = forceTopicMaxMessageSize;
    }

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

    public HandlersChainParameters toHandlersChainParameters() {
        return new HandlersChainParameters(
                this.keepAliveHeader.enabled,
                this.keepAliveHeader.timeoutSeconds,
                this.authentication.enabled,
                this.authentication.mode,
                this.forceTopicMaxMessageSize,
                this.idleTimeout,
                this.longIdleTimeout
        );
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
}
