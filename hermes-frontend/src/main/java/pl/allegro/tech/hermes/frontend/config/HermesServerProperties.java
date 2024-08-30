package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.server.HermesServerParameters;

@ConfigurationProperties(prefix = "frontend.server")
public class HermesServerProperties implements HermesServerParameters {

  private int port = 8080;

  private String host = "0.0.0.0";

  private Duration readTimeout = Duration.ofMillis(2000);

  private Duration requestParseTimeout = Duration.ofMillis(5000);

  private int maxHeaders = 20;

  private int maxParameters = 10;

  private int maxCookies = 10;

  private int backlogSize = 10000;

  private int ioThreadsCount = Runtime.getRuntime().availableProcessors() * 2;

  private int workerThreadCount = 200;

  private boolean alwaysKeepAlive = false;

  private boolean keepAlive = false;

  private boolean requestDumperEnabled = false;

  private int bufferSize = 16384;

  private boolean gracefulShutdownEnabled = true;

  private Duration gracefulShutdownInitialWait = Duration.ofSeconds(10);

  private boolean http2Enabled = false;

  @Override
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public Duration getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
  }

  @Override
  public Duration getRequestParseTimeout() {
    return requestParseTimeout;
  }

  public void setRequestParseTimeout(Duration requestParseTimeout) {
    this.requestParseTimeout = requestParseTimeout;
  }

  @Override
  public int getMaxHeaders() {
    return maxHeaders;
  }

  public void setMaxHeaders(int maxHeaders) {
    this.maxHeaders = maxHeaders;
  }

  @Override
  public int getMaxParameters() {
    return maxParameters;
  }

  public void setMaxParameters(int maxParameters) {
    this.maxParameters = maxParameters;
  }

  @Override
  public int getMaxCookies() {
    return maxCookies;
  }

  public void setMaxCookies(int maxCookies) {
    this.maxCookies = maxCookies;
  }

  @Override
  public int getBacklogSize() {
    return backlogSize;
  }

  public void setBacklogSize(int backlogSize) {
    this.backlogSize = backlogSize;
  }

  @Override
  public int getIoThreadsCount() {
    return ioThreadsCount;
  }

  public void setIoThreadsCount(int ioThreadsCount) {
    this.ioThreadsCount = ioThreadsCount;
  }

  @Override
  public int getWorkerThreadCount() {
    return workerThreadCount;
  }

  public void setWorkerThreadCount(int workerThreadCount) {
    this.workerThreadCount = workerThreadCount;
  }

  @Override
  public boolean isAlwaysKeepAlive() {
    return alwaysKeepAlive;
  }

  public void setAlwaysKeepAlive(boolean alwaysKeepAlive) {
    this.alwaysKeepAlive = alwaysKeepAlive;
  }

  @Override
  public boolean isKeepAlive() {
    return keepAlive;
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  @Override
  public boolean isRequestDumperEnabled() {
    return requestDumperEnabled;
  }

  public void setRequestDumperEnabled(boolean requestDumperEnabled) {
    this.requestDumperEnabled = requestDumperEnabled;
  }

  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  @Override
  public boolean isGracefulShutdownEnabled() {
    return gracefulShutdownEnabled;
  }

  public void setGracefulShutdownEnabled(boolean gracefulShutdownEnabled) {
    this.gracefulShutdownEnabled = gracefulShutdownEnabled;
  }

  @Override
  public Duration getGracefulShutdownInitialWait() {
    return gracefulShutdownInitialWait;
  }

  public void setGracefulShutdownInitialWait(Duration gracefulShutdownInitialWait) {
    this.gracefulShutdownInitialWait = gracefulShutdownInitialWait;
  }

  @Override
  public boolean isHttp2Enabled() {
    return http2Enabled;
  }

  public void setHttp2Enabled(boolean http2Enabled) {
    this.http2Enabled = http2Enabled;
  }
}
