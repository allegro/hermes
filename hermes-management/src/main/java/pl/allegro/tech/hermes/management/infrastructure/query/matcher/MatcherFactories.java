package pl.allegro.tech.hermes.management.infrastructure.query.matcher;


import com.fasterxml.jackson.databind.JsonNode;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParserContext;

import java.util.EnumMap;
import java.util.Map;

import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.AND;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.EQ;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.IN;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.NE;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.NOT;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.OR;

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
        FACTORIES.put(EQ, new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, JsonNode node, QueryParserContext parser) {
                return new EqualityMatcher<>(path, parser.parseValue(node));
            }
        });
        FACTORIES.put(NE, new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, JsonNode node, QueryParserContext parser) {
                return new NotMatcher<>(new EqualityMatcher<>(path, parser.parseValue(node)));
            }
        });
        FACTORIES.put(IN, new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, JsonNode node, QueryParserContext parser) {
                return new InMatcher<>(path, parser.parseArrayValue(node));
            }
        });
        FACTORIES.put(NOT, new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, JsonNode node, QueryParserContext parser) {
                return new NotMatcher<>(parser.parseNode(node));
            }
        });
        FACTORIES.put(AND, new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, JsonNode node, QueryParserContext parser) {
                return new AndMatcher<>(parser.parseNodes(node));
            }
        });
        FACTORIES.put(OR, new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, JsonNode node, QueryParserContext parser) {
                return new OrMatcher<>(parser.parseNodes(node));
            }
        });
    }
}
