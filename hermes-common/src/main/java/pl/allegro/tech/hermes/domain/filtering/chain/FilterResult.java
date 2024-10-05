package pl.allegro.tech.hermes.domain.filtering.chain;

import static java.lang.String.format;
import static java.util.Optional.empty;

import com.google.common.base.Joiner;
import java.util.Optional;

public final class FilterResult {
  private final boolean filtered;
  private final Optional<String> filterType;
  private final Optional<String> message;
  private final Optional<Exception> cause;

  public static final FilterResult PASS = new FilterResult(false, empty(), empty(), empty());

  public static FilterResult failed(String filterType, String message) {
    return new FilterResult(true, Optional.of(filterType), Optional.ofNullable(message), empty());
  }

  public static FilterResult failed(String filterType, Exception exception) {
    return new FilterResult(true, Optional.of(filterType), empty(), Optional.ofNullable(exception));
  }

  private FilterResult(
      boolean filtered,
      Optional<String> filterType,
      Optional<String> message,
      Optional<Exception> cause) {
    this.filtered = filtered;
    this.filterType = filterType;
    this.message = message;
    this.cause = cause;
  }

  public boolean isFiltered() {
    return filtered;
  }

  public Optional<String> getFilterType() {
    return filterType;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<Exception> getCause() {
    return cause;
  }

  @Override
  public String toString() {
    return "["
        + Joiner.on(",")
            .skipNulls()
            .join(
                format("%s={%s}", "filtered", filtered),
                toString("filterType", filterType),
                toString("message", message),
                toString("cause", cause))
        + "]";
  }

  private String toString(String fieldName, Optional<?> value) {
    return value.map(v -> format("%s={%s}", fieldName, v)).orElse(null);
  }
}
