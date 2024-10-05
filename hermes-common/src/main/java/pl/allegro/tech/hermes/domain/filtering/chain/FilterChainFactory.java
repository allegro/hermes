package pl.allegro.tech.hermes.domain.filtering.chain;

import static java.util.stream.Stream.concat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.domain.filtering.MessageFilter;
import pl.allegro.tech.hermes.domain.filtering.MessageFilterSource;

public class FilterChainFactory {
  private final MessageFilterSource availableFilters;

  public FilterChainFactory(MessageFilterSource filters) {
    this.availableFilters = filters;
  }

  public FilterChain create(final List<MessageFilterSpecification> filters) {
    Stream<MessageFilter> globalFilters = availableFilters.getGlobalFilters().stream();
    Stream<MessageFilter> subscriptionFilters = filters.stream().map(availableFilters::compile);
    return new FilterChain(concat(globalFilters, subscriptionFilters).collect(Collectors.toList()));
  }
}
