package pl.allegro.tech.hermes.management.infrastructure.query.matcher;


import pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator;

import java.util.EnumMap;
import java.util.Map;

import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.*;

public class MatcherFactories {

    private final static Map<Operator, MatcherFactory> FACTORIES = new EnumMap<>(Operator.class);

    static {
        registerFactories();
    }

    public static MatcherFactory getMatcherFactory(String operator) {
        try {
            return getMatcherFactory(Operator.from(operator));
        } catch (IllegalArgumentException e) {
            throw new MatcherNotFoundException(
                    String.format("Unrecognized operator: '%s'", operator)
            );
        }
    }

    public static MatcherFactory defaultMatcher() {
        return getMatcherFactory(EQ);
    }

    private static MatcherFactory getMatcherFactory(Operator operator) {
        return FACTORIES.get(operator);
    }

    private static void registerFactories() {
        FACTORIES.put(EQ, (path, node, parser) -> new EqualityMatcher(path, parser.parseValue(node)));
        FACTORIES.put(NE, (path, node, parser) -> new NotMatcher(new EqualityMatcher(path, parser.parseValue(node))));
        FACTORIES.put(LIKE, (path, node, parser) -> new LikeMatcher(path, parser.parseValue(node)));
        FACTORIES.put(IN, (path, node, parser) -> new InMatcher(path, parser.parseArrayValue(node)));
        FACTORIES.put(NOT, (path, node, parser) -> new NotMatcher(parser.parseNode(node)));
        FACTORIES.put(AND, (path, node, parser) -> new AndMatcher(parser.parseArrayNodes(node)));
        FACTORIES.put(OR, (path, node, parser) -> new OrMatcher(parser.parseArrayNodes(node)));
        FACTORIES.put(GRATER_THAN, (path, node, parser) -> new GraterThanMatcher(path, parser.parseValue(node)));
        FACTORIES.put(LOWER_THAN, (path, node, parser) -> new LowerThanMatcher(path, parser.parseValue(node)));
    }
}
