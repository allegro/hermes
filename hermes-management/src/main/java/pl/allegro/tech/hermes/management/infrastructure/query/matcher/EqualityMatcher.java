package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import org.apache.commons.jxpath.JXPathException;
import pl.allegro.tech.hermes.management.infrastructure.query.graph.ObjectGraph;

public class EqualityMatcher implements Matcher {

    private final String attribute;

    private final Object expected;

    public EqualityMatcher(String attribute, Object expected) {
        this.attribute = attribute;
        this.expected = expected;
    }

    @Override
    public boolean match(Object value) {
        try {
            if (expected == null) {
                return false;
            }
            Object actual = ObjectGraph.from(value).navigate(attribute).value();
            return expected.equals(actual)
                    || asString(expected).equals(asString(actual));
        } catch (JXPathException e) {
            throw new MatcherException(String.format("Could not navigate to specific path: '%s'", attribute), e);
        }
    }

    private static String asString(Object value) {
        return String.valueOf(value);
    }
}
