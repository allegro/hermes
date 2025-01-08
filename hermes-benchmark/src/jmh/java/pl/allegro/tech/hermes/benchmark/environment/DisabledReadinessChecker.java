package pl.allegro.tech.hermes.benchmark.environment;

import pl.allegro.tech.hermes.frontend.readiness.ReadinessChecker;

class DisabledReadinessChecker implements ReadinessChecker {

  private boolean isReady;

  public DisabledReadinessChecker(boolean isReady) {
    this.isReady = isReady;
  }

  @Override
  public boolean isReady() {
    return this.isReady;
  }

  @Override
  public void start() {
    this.isReady = true;
  }

  @Override
  public void stop() {
    this.isReady = false;
  }
}
