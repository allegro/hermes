package pl.allegro.tech.hermes.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import org.apache.commons.codec.digest.DigestUtils;

public class NodePassword {
  private static final int DEFAULT_STRING_LENGTH = 12;

  private final byte[] hashedPassword;

  @JsonCreator
  public NodePassword(@JsonProperty("hashedPassword") byte[] hashedPassword) {
    this.hashedPassword = Arrays.copyOf(hashedPassword, hashedPassword.length);
  }

  public NodePassword(String password) {
    this.hashedPassword = NodePassword.hashString(password);
  }

  public byte[] getHashedPassword() {
    return Arrays.copyOf(hashedPassword, hashedPassword.length);
  }

  private static byte[] hashString(String string) {
    return DigestUtils.sha256(string);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (o instanceof String) {
      return this.equals(NodePassword.fromString((String) o));
    }

    if (getClass() != o.getClass()) {
      return false;
    }

    NodePassword that = (NodePassword) o;

    return Arrays.equals(hashedPassword, that.hashedPassword);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(hashedPassword);
  }

  public static NodePassword fromString(String string) {
    return new NodePassword(string);
  }
}
