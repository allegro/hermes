package pl.allegro.tech.hermes.management.domain.subscription;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicNotEmptyException;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.subscription.commands.RemoveSubscriptionRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.command.ScheduleConsumerGroupToDeleteCommand;

public class SubscriptionRemover {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionRemover.class);
  private final Auditor auditor;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;
  private final SubscriptionOwnerCache subscriptionOwnerCache;
  private final SubscriptionRepository subscriptionRepository;
  private final boolean scheduleConsumerGroupRemoval;
  private final Clock clock;

  public SubscriptionRemover(
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      SubscriptionOwnerCache subscriptionOwnerCache,
      SubscriptionRepository subscriptionRepository,
      boolean scheduleConsumerGroupRemoval,
      Clock clock) {
    this.auditor = auditor;
    this.multiDcExecutor = multiDcExecutor;
    this.subscriptionOwnerCache = subscriptionOwnerCache;
    this.subscriptionRepository = subscriptionRepository;
    this.scheduleConsumerGroupRemoval = scheduleConsumerGroupRemoval;
    this.clock = clock;
  }

  public void removeSubscription(
      TopicName topicName, String subscriptionName, RequestUser removedBy) {
    auditor.beforeObjectRemoval(
        removedBy.getUsername(), Subscription.class.getSimpleName(), subscriptionName);
    Subscription subscription =
        subscriptionRepository.getSubscriptionDetails(topicName, subscriptionName);
    multiDcExecutor.executeByUser(
        new RemoveSubscriptionRepositoryCommand(topicName, subscriptionName), removedBy);

    if (scheduleConsumerGroupRemoval) {
      multiDcExecutor.executeByUser(
          new ScheduleConsumerGroupToDeleteCommand(
              new SubscriptionName(subscriptionName, topicName), Instant.now(clock)),
          removedBy);
    }

    auditor.objectRemoved(removedBy.getUsername(), subscription);
    subscriptionOwnerCache.onRemovedSubscription(subscriptionName, topicName);
  }

  public void removeSubscriptionRelatedToTopic(Topic topic, RequestUser removedBy) {
    List<Subscription> subscriptions = subscriptionRepository.listSubscriptions(topic.getName());
    ensureSubscriptionsHaveAutoRemove(subscriptions, topic.getName());
    logger.info(
        "Removing subscriptions of topic: {}, subscriptions: {}", topic.getName(), subscriptions);
    long start = System.currentTimeMillis();
    subscriptions.forEach(sub -> removeSubscription(topic.getName(), sub.getName(), removedBy));
    logger.info(
        "Removed subscriptions of topic: {} in {} ms",
        topic.getName(),
        System.currentTimeMillis() - start);
  }

  private void ensureSubscriptionsHaveAutoRemove(
      List<Subscription> subscriptions, TopicName topicName) {
    boolean anySubscriptionWithoutAutoRemove =
        subscriptions.stream().anyMatch(sub -> !sub.isAutoDeleteWithTopicEnabled());

    if (anySubscriptionWithoutAutoRemove) {
      logger.info("Cannot remove topic due to connected subscriptions");
      throw new TopicNotEmptyException(topicName);
    }
  }
}
