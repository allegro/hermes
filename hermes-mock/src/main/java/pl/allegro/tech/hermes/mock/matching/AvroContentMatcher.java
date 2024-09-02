package pl.allegro.tech.hermes.mock.matching;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.mock.HermesMockException;
import pl.allegro.tech.hermes.mock.HermesMockHelper;

class AvroContentMatcher<T> implements ValueMatcher<Request> {

  private final Predicate<T> predicate;
  private final Schema schema;
  private final Class<T> clazz;
  private final HermesMockHelper hermesMockHelper;

  AvroContentMatcher(
      HermesMockHelper hermesMockHelper, Predicate<T> predicate, Schema schema, Class<T> clazz) {
    this.hermesMockHelper = hermesMockHelper;
    this.predicate = predicate;
    this.schema = schema;
    this.clazz = clazz;
  }

  @Override
  public MatchResult match(Request actual) {
    try {
      T body = this.hermesMockHelper.deserializeAvro(actual.getBody(), schema, clazz);
      return MatchResult.of(predicate.test(body));
    } catch (HermesMockException exception) {
      return MatchResult.noMatch();
    }
  }
}
