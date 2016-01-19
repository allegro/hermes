package pl.allegro.tech.hermes.management.infrastructure.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.common.query.Query;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.Matcher;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MatcherQuery<T> implements Query<T> {

    private final Matcher matcher;
    private final ObjectMapper objectMapper;

    private MatcherQuery(Matcher matcher, ObjectMapper objectMapper) {
        this.matcher = matcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public Stream<T> filter(Stream<T> input) {
        return input.filter(getPredicate());
    }

    public Predicate<T> getPredicate() {
        return (value) -> matcher.match(convertToMap(value));
    }

    @SuppressWarnings("unchecked")
    //workaround for type which is not java bean
    private Map convertToMap(T value) {
        return objectMapper.convertValue(value, Map.class);
    }

    public static <T> Query<T> fromMatcher(Matcher matcher, ObjectMapper objectMapper) {
        return new MatcherQuery<>(matcher, objectMapper);
    }
}
