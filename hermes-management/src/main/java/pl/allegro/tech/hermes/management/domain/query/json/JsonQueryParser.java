package pl.allegro.tech.hermes.management.domain.query.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import pl.allegro.tech.hermes.management.domain.query.ParseException;
import pl.allegro.tech.hermes.management.domain.query.Query;
import pl.allegro.tech.hermes.management.domain.query.QueryParser;
import pl.allegro.tech.hermes.management.domain.query.matcher.AndMatcher;
import pl.allegro.tech.hermes.management.domain.query.matcher.Matcher;
import pl.allegro.tech.hermes.management.domain.query.matcher.MatcherNotFoundException;
import pl.allegro.tech.hermes.management.domain.query.matcher.Matchers;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JsonQueryParser implements QueryParser {

    private static final String QUERY = "query";

    private final ObjectMapper objectMapper;

    public JsonQueryParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Query<T> parse(String query, Class<T> type) {
        try {
            return parseDocument(
                    objectMapper.readTree(query)
            );
        } catch (IOException | MatcherNotFoundException e) {
            throw new ParseException("Query could not be parsed", e);
        }
    }

    private <T> Query<T> parseDocument(JsonNode document) {
        if (!document.isObject() || !document.has(QUERY)) {
            throw new ParseException("JSON object must contain 'query' attribute");
        }
        return parseQuery(document.get(QUERY));
    }

    private <T> Query<T> parseQuery(JsonNode query) {
        List<Matcher<T>> matchers = Lists.newArrayList();
        query.fields().forEachRemaining(entry ->
                matchers.add(Matchers.fromJsonAttribute(entry.getKey(), entry.getValue())));
        return new MatcherQuery<>(new AndMatcher<>(matchers));
    }

    private static class MatcherQuery<T> implements Query<T> {

        private final Matcher<T> matcher;

        private MatcherQuery(Matcher<T> matcher) {
            this.matcher = matcher;
        }

        @Override
        public Stream<T> filter(Collection<T> input) {
            return input.stream().filter(toPredicate());
        }

        @Override
        public Predicate<T> toPredicate() {
            return matcher::match;
        }
    }
}
