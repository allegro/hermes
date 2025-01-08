package pl.allegro.tech.hermes.frontend.readiness;

public interface ReadinessChecker {

  boolean isReady();

  void start();

  void stop() throws InterruptedException;
}
