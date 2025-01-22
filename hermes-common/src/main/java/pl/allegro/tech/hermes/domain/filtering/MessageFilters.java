package pl.allegro.tech.hermes.domain.filtering;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;

public class MessageFilters implements MessageFilterSource {
  private final Map<String, SubscriptionMessageFilterCompiler> filters;
  private final List<MessageFilter> globalFilters;

  public MessageFilters(
      List<MessageFilter> globalFilters,
      List<SubscriptionMessageFilterCompiler> subscriptionFilterCompilers) {
    this.globalFilters = globalFilters;
    this.filters =
        subscriptionFilterCompilers.stream()
            .collect(toMap(SubscriptionMessageFilterCompiler::getType, identity()));
  }

  @Override
  public MessageFilter compile(MessageFilterSpecification specification) {
    if (!filters.containsKey(specification.getType())) {
      throw new NoSuchFilterException(specification.getType());
    }
    return filters.get(specification.getType()).getMessageFilter(specification);
  }

  @Override
  public List<MessageFilter> getGlobalFilters() {
    return globalFilters;
  }
}
