package pl.allegro.tech.hermes.api;

import java.util.Objects;

public class Owner {

  private final String id;
  private final String name;
  private final String url;

  public Owner() {
    this.id = null;
    this.name = null;
    this.url = null;
  }

  public Owner(String id, String name) {
    this.id = id;
    this.name = name;
    this.url = null;
  }

  public Owner(String id, String name, String url) {
    this.id = id;
    this.name = name;
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Owner owner = (Owner) o;
    return Objects.equals(id, owner.id)
        && Objects.equals(name, owner.name)
        && Objects.equals(url, owner.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, url);
  }

  @Override
  public String toString() {
    return "Owner{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", url='" + url + '\'' + '}';
  }
}
