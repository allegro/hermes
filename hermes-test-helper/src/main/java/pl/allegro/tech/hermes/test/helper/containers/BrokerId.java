package pl.allegro.tech.hermes.test.helper.containers;

import java.util.Objects;

public class BrokerId {
  private final int id;

  public BrokerId(int id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BrokerId brokerId = (BrokerId) o;
    return id == brokerId.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
