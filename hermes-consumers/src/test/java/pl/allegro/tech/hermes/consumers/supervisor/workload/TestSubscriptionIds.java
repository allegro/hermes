package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;

public class TestSubscriptionIds implements SubscriptionIds {

  private final Map<SubscriptionName, SubscriptionId> nameToIdMap = new ConcurrentHashMap<>();
  private final Map<Long, SubscriptionId> valueToIdMap = new ConcurrentHashMap<>();

  public TestSubscriptionIds(List<SubscriptionId> ids) {
    ids.forEach(
        id -> {
          nameToIdMap.put(id.getSubscriptionName(), id);
          valueToIdMap.put(id.getValue(), id);
        });
  }

  @Override
  public Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName) {
    return Optional.ofNullable(nameToIdMap.get(subscriptionName));
  }

  @Override
  public Optional<SubscriptionId> getSubscriptionId(long id) {
    return Optional.ofNullable(valueToIdMap.get(id));
  }

  @Override
  public void start() {}
}
