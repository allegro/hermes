package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.Comparator.comparing;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import pl.allegro.tech.hermes.api.SubscriptionName;

class ConsumerTask {

  static Comparator<ConsumerTask> HEAVIEST_TASK_FIRST =
      comparing(ConsumerTask::getWeight).reversed();

  private final SubscriptionName subscriptionName;
  private final Instant lastRebalanceTimestamp;
  private final Weight weight;

  ConsumerTask(SubscriptionName subscriptionName, SubscriptionProfile subscriptionProfile) {
    this.subscriptionName = subscriptionName;
    this.lastRebalanceTimestamp = subscriptionProfile.getLastRebalanceTimestamp();
    this.weight = subscriptionProfile.getWeight();
  }

  SubscriptionName getSubscriptionName() {
    return subscriptionName;
  }

  Weight getWeight() {
    return weight;
  }

  Instant getLastRebalanceTimestamp() {
    return lastRebalanceTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerTask that = (ConsumerTask) o;
    return Objects.equals(subscriptionName, that.subscriptionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subscriptionName);
  }
}
