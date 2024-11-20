package pl.allegro.tech.hermes.test.helper.environment;

public interface HermesTestApp {

  HermesTestApp start();

  void stop();

  default void reset() {}
  ;

  boolean shouldBeRestarted();

  void restoreDefaultSettings();

  int getPort();
}
