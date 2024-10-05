package pl.allegro.tech.hermes.consumers.test;

public final class Wait {

  private Wait() {}

  public static void forCacheInvalidation() {
    sleep(700);
  }

  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException exception) {
      throw new RuntimeException("Who dares to interrupt me?", exception);
    }
  }
}
