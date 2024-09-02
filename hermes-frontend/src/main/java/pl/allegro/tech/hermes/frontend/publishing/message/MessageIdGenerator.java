package pl.allegro.tech.hermes.frontend.publishing.message;

import java.util.UUID;

public class MessageIdGenerator {

  public static String generate() {
    return UUID.randomUUID().toString();
  }
}
