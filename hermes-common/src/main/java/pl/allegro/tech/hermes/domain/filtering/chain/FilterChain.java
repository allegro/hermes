package pl.allegro.tech.hermes.domain.filtering.chain;

import java.util.ArrayList;
import java.util.List;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.domain.filtering.MessageFilter;

public final class FilterChain {
  private final List<MessageFilter> messageFilters;

  FilterChain(final List<MessageFilter> messageFilters) {
    this.messageFilters = new ArrayList<>(messageFilters);
  }

  public FilterResult apply(final FilterableMessage message) {
    for (MessageFilter filter : messageFilters) {
      try {
        if (!filter.test(message)) {
          return FilterResult.failed(filter.getType(), "logical");
        }
      } catch (Exception ex) {
        return FilterResult.failed(filter.getType(), ex);
      }
    }
    return FilterResult.PASS;
  }
}
