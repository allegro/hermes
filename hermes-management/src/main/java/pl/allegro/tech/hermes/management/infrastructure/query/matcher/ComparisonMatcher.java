package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import org.apache.commons.jxpath.JXPathException;
import pl.allegro.tech.hermes.management.infrastructure.query.graph.ObjectGraph;

import java.util.Optional;

import static java.lang.Double.parseDouble;

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
    public boolean match(Object object) {
        Object leftSideValue = extractAttributeValue(object);

        Optional<Double> leftSideValueAsNumber = tryParseNumber(leftSideValue);
        Optional<Double> rightSideValueAsNumber = tryParseNumber(rightSideValue);

        if (!leftSideValueAsNumber.isPresent()) {
            return true;
        }
        if (!rightSideValueAsNumber.isPresent()) {
            throw new MatcherInputException("Comparison operator requires numerical data");
        }
        return makeComparison(leftSideValueAsNumber.get(), rightSideValueAsNumber.get());
    }

    private Object extractAttributeValue(Object object) {
        try {
            Object value = ObjectGraph.from(object).navigate(attribute).value();

            if (value == null)
                throw new MatcherInputException(String.format("Cannot find '%s' attribute", this.attribute));
            return value;
        } catch (JXPathException e) {
            throw new MatcherException(String.format("Could not navigate to specific path: '%s'", attribute), e);
        }
    }

    private Optional<Double> tryParseNumber(Object value){
        try {
            return Optional.of(parseDouble(asString(value)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean makeComparison(Double leftSideNumber, Double rightSideNumber) {
        return operator.compare(leftSideNumber, rightSideNumber);
    }

    private String asString(Object value) {
        return String.valueOf(value);
    }
}
