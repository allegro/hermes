package pl.allegro.tech.hermes.management.infrastructure.query.graph;

public class ObjectGraph {

  private final Object target;

  private ObjectGraph(Object target) {
    this.target = target;
  }

  public ObjectAttribute navigate(String path) {
    return new JXPathAttribute(target, path);
  }

  public static ObjectGraph from(Object target) {
    return new ObjectGraph(target);
  }
}
