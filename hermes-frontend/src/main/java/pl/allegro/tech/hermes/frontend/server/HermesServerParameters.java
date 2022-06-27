package pl.allegro.tech.hermes.frontend.server;

public class HermesServerParameters {

    private final int port;

    private final String host;

    private final int readTimeout;

    private final int requestParseTimeout;

    private final int maxHeaders;

    private final int maxParameters;

    private final int maxCookies;

    private final int backlogSize;

    private final int ioThreadsCount;

    private final int workerThreadCount;

    private final boolean alwaysKeepAlive;

    private final boolean keepAlive;

    private final boolean requestDumperEnabled;

    private final int bufferSize;

    private final boolean gracefulShutdownEnabled;

    private final int gracefulShutdownInitialWaitMs;

    private final boolean http2Enabled;

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public int getReadTimeout() {
        return readTimeout;
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

    public int getMaxCookies() {
        return maxCookies;
    }

    public int getBacklogSize() {
        return backlogSize;
    }

    public int getIoThreadsCount() {
        return ioThreadsCount;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public boolean isAlwaysKeepAlive() {
        return alwaysKeepAlive;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public boolean isRequestDumperEnabled() {
        return requestDumperEnabled;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public boolean isGracefulShutdownEnabled() {
        return gracefulShutdownEnabled;
    }

    public int getGracefulShutdownInitialWaitMs() {
        return gracefulShutdownInitialWaitMs;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public HermesServerParameters(int port,
                                  String host,
                                  int readTimeout,
                                  int requestParseTimeout,
                                  int maxHeaders,
                                  int maxParameters,
                                  int maxCookies,
                                  int backlogSize,
                                  int ioThreadsCount,
                                  int workerThreadCount,
                                  boolean alwaysKeepAlive,
                                  boolean keepAlive,
                                  boolean requestDumperEnabled,
                                  int bufferSize,
                                  boolean gracefulShutdownEnabled,
                                  int gracefulShutdownInitialWaitMs,
                                  boolean http2Enabled) {
        this.port = port;
        this.host = host;
        this.readTimeout = readTimeout;
        this.requestParseTimeout = requestParseTimeout;
        this.maxHeaders = maxHeaders;
        this.maxParameters = maxParameters;
        this.maxCookies = maxCookies;
        this.backlogSize = backlogSize;
        this.ioThreadsCount = ioThreadsCount;
        this.workerThreadCount = workerThreadCount;
        this.alwaysKeepAlive = alwaysKeepAlive;
        this.keepAlive = keepAlive;
        this.requestDumperEnabled = requestDumperEnabled;
        this.bufferSize = bufferSize;
        this.gracefulShutdownEnabled = gracefulShutdownEnabled;
        this.gracefulShutdownInitialWaitMs = gracefulShutdownInitialWaitMs;
        this.http2Enabled = http2Enabled;
    }
}
