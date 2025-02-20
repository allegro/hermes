package pl.allegro.tech.hermes.frontend.server;

import java.time.Duration;

public interface HermesServerParameters {

  int getPort();

  String getHost();

  Duration getReadTimeout();

  Duration getRequestParseTimeout();

  int getMaxHeaders();

  int getMaxParameters();

  int getMaxCookies();

  int getBacklogSize();

  int getIoThreadsCount();

  int getWorkerThreadCount();

  boolean isAlwaysKeepAlive();

  boolean isKeepAlive();

  boolean isRequestDumperEnabled();

  int getBufferSize();

  boolean isGracefulShutdownEnabled();

  Duration getGracefulShutdownInitialWait();

  boolean isHttp2Enabled();
}
