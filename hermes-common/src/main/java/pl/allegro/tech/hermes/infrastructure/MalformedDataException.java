package pl.allegro.tech.hermes.infrastructure;

public class MalformedDataException extends RuntimeException {

  public MalformedDataException(String path, Throwable throwable) {
    super(String.format("Unable to read data from path %s", path), throwable);
  }
}
