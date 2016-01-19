package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import org.apache.commons.jxpath.JXPathException;
import pl.allegro.tech.hermes.management.infrastructure.query.graph.ObjectGraph;

import java.util.Arrays;

public class InMatcher implements Matcher {

    private final String attribute;

    private final Object[] values;

    public InMatcher(String attribute, Object[] values) {
        this.attribute = attribute;
        this.values = values;
    }

    @Override
    public boolean match(Object value) {

        try {
            if (values == null || values.length == 0) {
                return false;
            }
            Object actual = ObjectGraph.from(value).navigate(attribute).value();
            return actual != null && (contains(actual) || contains(asString(actual)));
        } catch (JXPathException e) {
            throw new MatcherException(String.format("Could not navigate to specific path: '%s'", attribute), e);
        }
    }

    private boolean contains(Object actual) {
        return Arrays.stream(values)
                .anyMatch(actual::equals);
    }

    private String asString(Object actual) {
        return String.valueOf(actual);
    }
}
