package pl.allegro.tech.hermes.test.helper.environment;

public interface Starter<T> {

  void start() throws Exception;

  void stop() throws Exception;

  T instance();
}
