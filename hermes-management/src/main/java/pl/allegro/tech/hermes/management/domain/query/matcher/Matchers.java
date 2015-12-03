package pl.allegro.tech.hermes.management.domain.query.matcher;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static pl.allegro.tech.hermes.management.domain.query.Operators.EQ;

public class Matchers {

    private final static Map<String, MatcherFactory> FACTORIES = Maps.newConcurrentMap();
    private final static MatcherFactory DEFAULT_OPERATOR_FACTORY;

    static {
        DEFAULT_OPERATOR_FACTORY = new MatcherFactory() {
            @Override
            public <T> Matcher<T> createMatcher(String path, Object value) {
                return new EqualityMatcher<>(path, value);
            }
        };
        FACTORIES.put(EQ, DEFAULT_OPERATOR_FACTORY);
    }

    public static <T> Matcher<T> fromJsonAttribute(String key, JsonNode node) {

        if (node.isObject()) {
            List<Map.Entry<String, JsonNode>> attributes = Lists.newArrayList(node.fields());
            if (attributes.size() != 1) {
                throw new MatcherNotFoundException("The object must define exactly one operator");
            }
            Map.Entry<String, JsonNode> entry = attributes.get(0);
            return getMatcher(entry.getKey(), key, entry.getValue());
        } else {
            return DEFAULT_OPERATOR_FACTORY.createMatcher(key, getValue(node));
        }
    }

    private static <T> Matcher<T> getMatcher(String operator, String path, JsonNode value) {
        if (!FACTORIES.containsKey(operator)) {
            throw new MatcherNotFoundException(
                    String.format("No matcher for operation '%s' could be found.", operator)
            );
        }
        return FACTORIES.get(operator).createMatcher(path, getValue(value));
    }

    private static Object getValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.asInt();
        }
        return null;
    }
}
