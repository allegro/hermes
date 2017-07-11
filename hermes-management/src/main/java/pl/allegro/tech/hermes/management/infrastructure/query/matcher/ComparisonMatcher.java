package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import org.apache.commons.jxpath.JXPathException;
import pl.allegro.tech.hermes.management.infrastructure.query.graph.ObjectGraph;

public class ComparisonMatcher implements Matcher {

    private final String attribute;

    private final Object rightSideValue;

    private final ComparisonOperator operator;

    public ComparisonMatcher(String attribute, Object rightSideValue, ComparisonOperator operator) {
        this.attribute = attribute;
        this.rightSideValue = rightSideValue;
        this.operator = operator;
    }

    @Override
    public boolean match(Object value) {
        Object leftSideValue = getUserInput(value);

        double leftSideValueAsNumber = parseToNumber(leftSideValue);
        double rightSideValueAsNumber = parseToNumber(rightSideValue);

        return makeComparison(leftSideValueAsNumber, rightSideValueAsNumber);
    }

    private Object getUserInput(Object value) {
        try {
            Object userInput = ObjectGraph.from(value).navigate(attribute).value();

            if (userInput == null)
                throw new MatcherInputException(String.format("Cannot find '%s' attribute", this.attribute));
            return userInput;
        }
        catch (JXPathException e) {
            throw new MatcherException(String.format("Could not navigate to specific path: '%s'", attribute), e);
        }
    }

    private Double parseToNumber(Object value){
        try {
            return Double.parseDouble(asString(value));
        }
        catch (NumberFormatException e) {
            throw new MatcherInputException("Comparison operator requires numerical data");
        }
    }

    private boolean makeComparison(Double leftSideNumber, Double rightSideNumber) {
        return operator.compare(leftSideNumber, rightSideNumber);
    }

    private String asString(Object value) {
        return String.valueOf(value);
    }
}
