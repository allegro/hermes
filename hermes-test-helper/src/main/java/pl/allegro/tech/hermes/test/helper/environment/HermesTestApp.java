package pl.allegro.tech.hermes.test.helper.environment;

public interface HermesTestApp {

  HermesTestApp start();

  void stop();

  boolean shouldBeRestarted();

  void restoreDefaultSettings();

  int getPort();
}
