package pl.allegro.tech.hermes.domain.filtering.json;

import static pl.allegro.tech.hermes.domain.filtering.FilteringException.check;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.domain.filtering.FilteringException;
import pl.allegro.tech.hermes.domain.filtering.MatchingStrategy;
import pl.allegro.tech.hermes.domain.filtering.UnsupportedMatchingStrategyException;

class JsonPathPredicate implements Predicate<FilterableMessage> {
  private final Configuration configuration;
  private final String path;
  private final Pattern matcher;
  private final MatchingStrategy matchingStrategy;

  JsonPathPredicate(
      String path,
      Pattern matcher,
      Configuration configuration,
      MatchingStrategy matchingStrategy) {
    this.path = path;
    this.matcher = matcher;
    this.configuration = configuration;
    this.matchingStrategy = matchingStrategy;
  }

  @Override
  public boolean test(FilterableMessage message) {
    check(
        message.getContentType() == ContentType.JSON,
        "This filter supports only JSON contentType.");
    try {
      List<Object> result =
          JsonPath.parse(new ByteArrayInputStream(message.getData()), configuration).read(path);
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
