package pl.allegro.tech.hermes.consumers.consumer.interpolation;

public class InterpolationException extends Exception {
  public InterpolationException(String format, Throwable t) {
    super(format, t);
  }
}
