package pl.allegro.tech.hermes.frontend.buffer.chronicle;

public class ChronicleMapCreationException extends RuntimeException {

  public ChronicleMapCreationException(Exception e) {
    super("Exception while creating ChronicleMap", e);
  }
}
