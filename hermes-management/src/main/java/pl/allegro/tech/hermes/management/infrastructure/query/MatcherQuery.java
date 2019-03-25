package pl.allegro.tech.hermes.management.infrastructure.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.Matcher;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.MatcherException;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.MatcherInputException;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MatcherQuery<T> implements Query<T> {

    private static final Logger logger = LoggerFactory.getLogger(MatcherQuery.class);

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
        return (value) -> {
            try {
                return matcher.match(convertToMap(value));
            } catch (MatcherException e) {
                logger.info("Failed to match {}, skipping", value, e);
                return false;
            } catch (MatcherInputException e) {
                logger.error("Not existing query property", e);
                throw new IllegalArgumentException(e);
            }
        };
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
