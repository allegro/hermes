package pl.allegro.tech.hermes.management.domain.search.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

public class NotificationBasedSearchCache
    implements SearchCache, TopicCallback, SubscriptionCallback {

  private static final Logger logger = LoggerFactory.getLogger(NotificationBasedSearchCache.class);

  private final ConcurrentMap<String, CachedTopicItem> topicCache = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, CachedSubscriptionItem> subscriptionCache =
      new ConcurrentHashMap<>();

  private final TopicRepository topicRepository;
  private final SubscriptionRepository subscriptionRepository;

  public NotificationBasedSearchCache(
      InternalNotificationsBus notificationsBus,
      TopicRepository topicRepository,
      SubscriptionRepository subscriptionRepository) {
    this.topicRepository = topicRepository;
    this.subscriptionRepository = subscriptionRepository;
    notificationsBus.registerTopicCallback(this);
    notificationsBus.registerSubscriptionCallback(this);
    initialize();
  }

  @Override
  public Stream<CachedItem> getAllItems() {
    return Stream.concat(topicCache.values().stream(), subscriptionCache.values().stream());
  }

  @Override
  public void initialize() {
    initializeTopicsCache();
    initializeSubscriptionsCache();
  }

  @Override
  public void onTopicCreated(Topic topic) {
    putTopicInCache(topic);
  }

  @Override
  public void onTopicChanged(Topic topic) {
    putTopicInCache(topic);
  }

  @Override
  public void onTopicRemoved(Topic topic) {
    topicCache.remove(getTopicCacheKey(topic));
  }

  @Override
  public void onSubscriptionCreated(Subscription subscription) {
    putSubscriptionInCache(subscription);
  }

  @Override
  public void onSubscriptionRemoved(Subscription subscription) {
    subscriptionCache.remove(getSubscriptionCacheKey(subscription));
  }

  @Override
  public void onSubscriptionChanged(Subscription subscription) {
    putSubscriptionInCache(subscription);
  }

  private void initializeTopicsCache() {
    for (Topic topic : topicRepository.listAllTopics()) {
      topicCache.put(topic.getQualifiedName(), createCachedTopic(topic));
    }
    logger.info("Topic cache initialized. Items count: {}", topicCache.size());
  }

  private void initializeSubscriptionsCache() {
    for (Subscription subscription : subscriptionRepository.listAllSubscriptions()) {
      putSubscriptionInCache(subscription);
    }
    logger.info("Subscription cache initialized. Items count: {}", subscriptionCache.size());
  }

  private void putTopicInCache(Topic topic) {
    this.topicCache.put(getTopicCacheKey(topic), createCachedTopic(topic));
  }

  private String getTopicCacheKey(Topic topic) {
    return topic.getQualifiedName();
  }

  private void putSubscriptionInCache(Subscription subscription) {
    subscriptionCache.put(
        getSubscriptionCacheKey(subscription), createCachedSubscription(subscription));
  }

  private String getSubscriptionCacheKey(Subscription subscription) {
    return subscription.getQualifiedName().getQualifiedName();
  }

  private CachedTopicItem createCachedTopic(Topic topic) {
    return new CachedTopicItem(topic.getName().qualifiedName(), topic);
  }

  private CachedSubscriptionItem createCachedSubscription(Subscription subscription) {
    return new CachedSubscriptionItem(subscription.getName(), subscription);
  }
}
