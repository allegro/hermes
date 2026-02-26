package pl.allegro.tech.hermes.frontend.buffer.chronicle;

/**
 * @deprecated This feature is deprecated and will be removed in a future version.
 */
@Deprecated
public class ChronicleMapCreationException extends RuntimeException {

  public ChronicleMapCreationException(Exception e) {
    super("Exception while creating ChronicleMap", e);
  }
}
