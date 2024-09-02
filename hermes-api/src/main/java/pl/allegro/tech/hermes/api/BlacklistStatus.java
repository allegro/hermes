package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public final class BlacklistStatus {

  public static final BlacklistStatus BLACKLISTED = new BlacklistStatus(true);
  public static final BlacklistStatus NOT_BLACKLISTED = new BlacklistStatus(false);

  private final boolean blacklisted;

  @JsonCreator
  private BlacklistStatus(@JsonProperty("blacklisted") boolean blacklisted) {
    this.blacklisted = blacklisted;
  }

  public boolean isBlacklisted() {
    return blacklisted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlacklistStatus that = (BlacklistStatus) o;
    return blacklisted == that.blacklisted;
  }

  @Override
  public int hashCode() {
    return Objects.hash(blacklisted);
  }
}
