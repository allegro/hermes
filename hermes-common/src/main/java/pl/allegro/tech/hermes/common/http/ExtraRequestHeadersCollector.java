package pl.allegro.tech.hermes.common.http;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ExtraRequestHeadersCollector
    implements Collector<Map.Entry<String, String>, StringBuilder, String> {
  private ExtraRequestHeadersCollector() {}

  public static ExtraRequestHeadersCollector extraRequestHeadersCollector() {
    return new ExtraRequestHeadersCollector();
  }

  @Override
  public Supplier<StringBuilder> supplier() {
    return StringBuilder::new;
  }

  @Override
  public BiConsumer<StringBuilder, Map.Entry<String, String>> accumulator() {
    return (StringBuilder accumulator, Map.Entry<String, String> entry) -> {
      accumulator.append(entry.getKey());
      accumulator.append(": ");
      accumulator.append(entry.getValue());
      accumulator.append('\n');
    };
  }

  @Override
  public BinaryOperator<StringBuilder> combiner() {
    return StringBuilder::append;
  }

  @Override
  public Function<StringBuilder, String> finisher() {
    return (StringBuilder acc) -> {
      if (acc.length() > 0) {
        acc.setLength(acc.length() - 1);
      }
      return acc.toString();
    };
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Collections.emptySet();
  }
}
