package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import org.apache.commons.jxpath.JXPathException;
import pl.allegro.tech.hermes.management.infrastructure.query.graph.ObjectGraph;

public class LowerThanMatcher implements Matcher {

    private final String attribute;

    private final Object rightSideValue;

    public LowerThanMatcher(String attribute, Object rightSideValue) {
        this.attribute = attribute;
        this.rightSideValue = rightSideValue;
    }

    @Override
    public boolean match(Object value) {
        try {
            Object leftSideValue = ObjectGraph.from(value).navigate(attribute).value();

            if (leftSideValue == null) {
                throw new MatcherInputException(String.format("Cannot find '%s' attribute", this.attribute));
            }

            double leftSideValueAsNumber = Double.parseDouble(asString(leftSideValue));
            double rightSideValueAsNumber = Double.parseDouble(asString(rightSideValue));
            return leftSideValueAsNumber < rightSideValueAsNumber;
        }
        catch (JXPathException e) {
            throw new MatcherException(String.format("Could not navigate to specific path: '%s'", attribute), e);
        }
        catch (NumberFormatException e) {
            throw new MatcherInputException("Lower than operator requires numerical data");
        }
    }

    private String asString(Object value) {
        return String.valueOf(value);
    }
}
