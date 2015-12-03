package pl.allegro.tech.hermes.management.domain.query.graph;

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
        return JXPathContext.newContext(target).getValue(path);
    }
}
