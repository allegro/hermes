package pl.allegro.tech.hermes.consumers.subscription.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

public class NotificationsBasedSubscriptionCache
    implements SubscriptionsCache, SubscriptionCallback {

  private final Map<SubscriptionName, Subscription> subscriptions = new ConcurrentHashMap<>();

  private final GroupRepository groupRepository;

  private final TopicRepository topicRepository;

  private final SubscriptionRepository subscriptionRepository;

  public NotificationsBasedSubscriptionCache(
      InternalNotificationsBus notificationsBus,
      GroupRepository groupRepository,
      TopicRepository topicRepository,
      SubscriptionRepository subscriptionRepository) {
    notificationsBus.registerSubscriptionCallback(this);
    this.groupRepository = groupRepository;
    this.topicRepository = topicRepository;
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public void onSubscriptionCreated(Subscription subscription) {
    this.subscriptions.put(subscription.getQualifiedName(), subscription);
  }

  @Override
  public void onSubscriptionRemoved(Subscription subscription) {
    this.subscriptions.remove(subscription.getQualifiedName(), subscription);
  }

  @Override
  public void onSubscriptionChanged(Subscription subscription) {
    this.subscriptions.put(subscription.getQualifiedName(), subscription);
  }

  @Override
  public Subscription getSubscription(SubscriptionName subscriptionName) {
    return subscriptions.get(subscriptionName);
  }

  @Override
  public List<Subscription> subscriptionsOfTopic(TopicName topicName) {
    return subscriptions.values().stream()
        .filter(s -> s.getTopicName().equals(topicName))
        .collect(Collectors.toList());
  }

  @Override
  public List<SubscriptionName> listActiveSubscriptionNames() {
    return subscriptions.values().stream()
        .filter(Subscription::isActive)
        .map(Subscription::getQualifiedName)
        .collect(Collectors.toList());
  }

  @Override
  public void start() {
    for (String groupName : groupRepository.listGroupNames()) {
      for (String topicName : topicRepository.listTopicNames(groupName)) {
        for (Subscription subscription :
            subscriptionRepository.listSubscriptions(new TopicName(groupName, topicName))) {
          subscriptions.put(subscription.getQualifiedName(), subscription);
        }
      }
    }
  }
}
