package pl.allegro.tech.hermes.domain.filtering;

public class UnsupportedMatchingStrategyException extends FilteringException {
  public UnsupportedMatchingStrategyException(String filterType, MatchingStrategy strategy) {
    super(
        "Matching strategy '"
            + strategy
            + "' is not supported in filters of type '"
            + filterType
            + "'.");
  }
}
