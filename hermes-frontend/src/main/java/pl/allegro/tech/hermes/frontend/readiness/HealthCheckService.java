package pl.allegro.tech.hermes.frontend.readiness;

public class HealthCheckService {

  private volatile boolean shutdown = true;

  public boolean isShutdown() {
    return shutdown;
  }

  public void shutdown() {
    this.shutdown = true;
  }

  public void startup() {
    this.shutdown = false;
  }
}
