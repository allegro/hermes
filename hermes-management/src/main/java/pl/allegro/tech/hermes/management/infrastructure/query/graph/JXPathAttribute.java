package pl.allegro.tech.hermes.management.infrastructure.query.graph;

import org.apache.commons.jxpath.FunctionLibrary;
import org.apache.commons.jxpath.JXPathContext;

public class JXPathAttribute implements ObjectAttribute {

  private final Object target;

  private final String path;

  public JXPathAttribute(Object target, String path) {
    this.target = target;
    this.path = path.replace('.', '/');
  }

  @Override
  public Object value() {
    JXPathContext context = JXPathContext.newContext(target);
    context.setFunctions(new FunctionLibrary());
    return context.getValue(path);
  }
}
