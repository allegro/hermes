package pl.allegro.tech.hermes.consumers.consumer.filtering.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException;
import pl.allegro.tech.hermes.consumers.consumer.filtering.MatchingStrategy;
import pl.allegro.tech.hermes.consumers.consumer.filtering.UnsupportedMatchingStrategyException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException.check;

public class JsonPathPredicate implements Predicate<Message> {
    private Configuration configuration;
    private String path;
    private Pattern matcher;
    private MatchingStrategy matchingStrategy;

    public JsonPathPredicate(String path, Pattern matcher, Configuration configuration) {
       this(path, matcher, configuration, MatchingStrategy.ALL);
    }

    public JsonPathPredicate(String path, Pattern matcher, Configuration configuration, MatchingStrategy matchingStrategy) {
        this.path = path;
        this.matcher = matcher;
        this.configuration = configuration;
        this.matchingStrategy = matchingStrategy;
    }

    @Override
    public boolean test(Message message) {
        check(message.getContentType() == ContentType.JSON, "This filter supports only JSON contentType.");
        try {
            List<Object> result = JsonPath.parse(new ByteArrayInputStream(message.getData()), configuration).read(path);
            Stream<String> resultStream = result.stream().map(Object::toString);

            return !result.isEmpty() && matchResultsStream(resultStream);
        } catch (Exception ex) {
            throw new FilteringException(ex);
        }
    }

    private boolean matchResultsStream(Stream<String> results) {
        switch (matchingStrategy) {
            case ALL:
                return results.allMatch(this::matches);
            case ANY:
                return results.anyMatch(this::matches);
            default:
                throw new UnsupportedMatchingStrategyException("avropath", matchingStrategy);
        }
    }

    private boolean matches(String value) {
        return matcher.matcher(value).matches();
    }
}
