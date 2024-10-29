package pl.allegro.tech.hermes.mock.matching;

import java.util.function.Predicate;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.mock.HermesMockHelper;

public class ContentMatchers {

  public static <T> AvroContentMatcher<T> matchAvro(
      HermesMockHelper hermesMockHelper, Predicate<T> predicate, Schema schema, Class<T> clazz) {
    return new AvroContentMatcher<T>(hermesMockHelper, predicate, schema, clazz);
  }

  public static <T> JsonContentMatcher<T> matchJson(
      HermesMockHelper hermesMockHelper, Predicate<T> predicate, Class<T> clazz) {
    return new JsonContentMatcher<T>(hermesMockHelper, predicate, clazz);
  }
}
