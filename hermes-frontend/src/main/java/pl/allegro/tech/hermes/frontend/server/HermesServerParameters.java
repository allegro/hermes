package pl.allegro.tech.hermes.frontend.server;

public class HermesServerParameters {

    private final boolean topicMetadataRefreshJobEnabled;
    private final boolean gracefulShutdownEnabled;
    private final int gracefulShutdownInitialWaitMs;
    private final int requestParseTimeout;
    private final int maxHeaders;
    private final int maxCookies;
    private final int maxParameters;
    private final boolean alwaysSetKepAlive;
    private final boolean setKeepAlive;
    private final int backlogSize;
    private final int readTimeout;
    private final int ioThreadCount;
    private final int workerThreadCount;
    private final int bufferSize;
    private final boolean sslEnabled;
    private final String sslClientAuthMode;
    private final boolean http2Enabled;
    private final boolean requestDumper;
    private final int frontendPort;
    private final int frontendSslPort;
    private final String frontendHost;


    public HermesServerParameters(boolean topicMetadataRefreshJobEnabled,
                                  boolean gracefulShutdownEnabled,
                                  int gracefulShutdownInitialWaitMs,
                                  int requestParseTimeout,
                                  int maxHeaders,
                                  int maxCookies,
                                  int maxParameters,
                                  boolean alwaysSetKepAlive,
                                  boolean setKeepAlive,
                                  int backlogSize,
                                  int readTimeout,
                                  int ioThreadCount,
                                  int workerThreadCount,
                                  int bufferSize,
                                  boolean sslEnabled,
                                  String sslClientAuthMode,
                                  boolean http2Enabled,
                                  boolean requestDumper,
                                  int frontendPort,
                                  int frontendSslPort,
                                  String frontendHost) {
        this.topicMetadataRefreshJobEnabled = topicMetadataRefreshJobEnabled;
        this.gracefulShutdownEnabled = gracefulShutdownEnabled;
        this.gracefulShutdownInitialWaitMs = gracefulShutdownInitialWaitMs;
        this.requestParseTimeout = requestParseTimeout;
        this.maxHeaders = maxHeaders;
        this.maxCookies = maxCookies;
        this.maxParameters = maxParameters;
        this.alwaysSetKepAlive = alwaysSetKepAlive;
        this.setKeepAlive = setKeepAlive;
        this.backlogSize = backlogSize;
        this.readTimeout = readTimeout;
        this.ioThreadCount = ioThreadCount;
        this.workerThreadCount = workerThreadCount;
        this.bufferSize = bufferSize;
        this.sslEnabled = sslEnabled;
        this.sslClientAuthMode = sslClientAuthMode;
        this.http2Enabled = http2Enabled;
        this.requestDumper = requestDumper;
        this.frontendPort = frontendPort;
        this.frontendSslPort = frontendSslPort;
        this.frontendHost = frontendHost;
    }

    public boolean isTopicMetadataRefreshJobEnabled() {
        return topicMetadataRefreshJobEnabled;
    }

    public int getRequestParseTimeout() {
        return requestParseTimeout;
    }

    public int getMaxHeaders() {
        return maxHeaders;
    }

    public int getMaxParameters() {
        return maxParameters;
    }

    public boolean isAlwaysSetKepAlive() {
        return alwaysSetKepAlive;
    }

    public boolean isSetKeepAlive() {
        return setKeepAlive;
    }

    public int getBacklogSize() {
        return backlogSize;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getIoThreadCount() {
        return ioThreadCount;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public String getSslClientAuthMode() {
        return sslClientAuthMode;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public boolean isRequestDumper() {
        return requestDumper;
    }

    public boolean isGracefulShutdownEnabled() {
        return gracefulShutdownEnabled;
    }

    public int getGracefulShutdownInitialWaitMs() {
        return gracefulShutdownInitialWaitMs;
    }

    public int getMaxCookies() {
        return maxCookies;
    }

    public int getFrontendPort() {
        return frontendPort;
    }

    public int getFrontendSslPort() {
        return frontendSslPort;
    }

    public String getFrontendHost() {
        return frontendHost;
    }
}
