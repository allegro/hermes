package pl.allegro.tech.hermes.management.infrastructure.query.parser.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import pl.allegro.tech.hermes.common.query.Query;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.AndMatcher;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.Matcher;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.MatcherFactories;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.MatcherNotFoundException;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.ParseException;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParser;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParserContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static pl.allegro.tech.hermes.management.infrastructure.query.MatcherQuery.fromMatcher;

public class JsonQueryParser implements QueryParser, QueryParserContext {

    private static final String QUERY = "query";

    private final ObjectMapper objectMapper;

    public JsonQueryParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Query<T> parse(InputStream input, Class<T> type) {
        try {
            return parseDocument(
                    objectMapper.readTree(input)
            );
        } catch (IOException | MatcherNotFoundException e) {
            throw new ParseException("Query could not be parsed", e);
        }
    }

    @Override
    public <T> Query<T> parse(String query, Class<T> type) {
        return this.parse(new ByteArrayInputStream(query.getBytes()), type);
    }

    @Override
    public <T> Matcher<T> parseNode(JsonNode node) {
        Map.Entry<String, JsonNode> entry = singleNode(node);
        return parseSingleAttribute(entry.getKey(), entry.getValue());
    }

    @Override
    public <T> List<Matcher<T>> parseNodes(JsonNode node) {
        return parseAllAttributes(node);
    }

    @Override
    public Object parseValue(JsonNode node) {
        if(!node.isValueNode()) {
            throw new ParseException("The node value wasn't present");
        }
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isInt()) {
            return node.asInt();
        } else if(node.isDouble()) {
            return node.asDouble();
        }
        return null;
    }

    @Override
    public Object[] parseArrayValue(JsonNode node) {
        if(!node.isArray()) {
            throw new ParseException("Element value was expected to be an array");
        }
        return stream(node.spliterator(), false)
                .map(this::parseValue)
                .toArray();
    }

    private <T> Query<T> parseDocument(JsonNode document) {
        validateDocument(document);
        return parseQuery(document.get(QUERY));
    }

    private <T> Query<T> parseQuery(JsonNode node) {
        return fromMatcher(
                new AndMatcher<>(parseNodes(node))
        );
    }

    private <T> Matcher<T> parseSingleAttribute(String key, JsonNode node) {
        if(isOperator(key)) {
            return parseOperator(key, node);
        } else if (node.isObject()) {
            return parseObject(key, node);
        } else {
            return parseAttribute(key, node);
        }
    }

    private <T> List<Matcher<T>> parseAllAttributes(JsonNode node) {
        return stream(
                Spliterators.spliteratorUnknownSize(node.fields(), Spliterator.NONNULL), false)
                .map(new Function<Map.Entry<String, JsonNode>, Matcher<T>>() {
                    @Override
                    public Matcher<T> apply(Map.Entry<String, JsonNode> entry) {
                        return parseSingleAttribute(entry.getKey(), entry.getValue());
                    }
                })
                .collect(Collectors.toList());
    }

    private <T> Matcher<T> parseOperator(String key, JsonNode node) {
        return MatcherFactories.getMatcherFactory(key).createMatcher(key, node, this);
    }

    private <T> Matcher<T> parseObject(String key, JsonNode node) {
        Map.Entry<String, JsonNode> entry = singleNode(node);
        return MatcherFactories.getMatcherFactory(entry.getKey()).createMatcher(key, entry.getValue(), this);
    }

    private <T> Matcher<T> parseAttribute(String key, JsonNode node) {
        return MatcherFactories.defaultMatcher().createMatcher(key, node, this);
    }

    private boolean isOperator(String key) {
        return Operator.isValid(key);
    }

    private Map.Entry<String, JsonNode> singleNode(JsonNode node) {
        List<Map.Entry<String, JsonNode>> attributes = Lists.newArrayList(node.fields());
        if (attributes.size() != 1) {
            throw new MatcherNotFoundException(
                    String.format(
                            "The object must define exactly one member, but defines %s",
                            Lists.newArrayList(node.fieldNames()).toString()
                    )
            );
        }
        return attributes.get(0);
    }

    private void validateDocument(JsonNode document) {
        if (!document.isObject() || !document.has(QUERY)) {
            throw new ParseException("JSON object must contain 'query' attribute");
        }
    }
}
