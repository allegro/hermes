package pl.allegro.tech.hermes.consumers.subscription.id;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;

public class NotificationAwareSubscriptionIdsCache
    implements SubscriptionIds, SubscriptionCallback {

  private static final Logger logger =
      LoggerFactory.getLogger(NotificationAwareSubscriptionIdsCache.class);

  private final SubscriptionsCache subscriptionsCache;
  private final SubscriptionIdProvider subscriptionIdProvider;
  private final Map<SubscriptionName, SubscriptionId> nameToIdMap = new ConcurrentHashMap<>();
  private final Map<Long, SubscriptionId> valueToIdMap = new ConcurrentHashMap<>();
  private final Cache<SubscriptionName, SubscriptionId> nameToIdMapOfRemoved;
  private final Cache<Long, SubscriptionId> valueToIdMapOfRemoved;

  public NotificationAwareSubscriptionIdsCache(
      InternalNotificationsBus notificationsBus,
      SubscriptionsCache subscriptionsCache,
      SubscriptionIdProvider subscriptionIdProvider,
      long removedSubscriptionsExpireAfterAccessSeconds,
      Ticker ticker) {
    this.subscriptionsCache = subscriptionsCache;
    this.subscriptionIdProvider = subscriptionIdProvider;

    this.nameToIdMapOfRemoved =
        createExpiringCache(
            removedSubscriptionsExpireAfterAccessSeconds,
            ticker,
            notification ->
                logger.info(
                    "Removing expired subscription {} id from name->id cache",
                    notification.getKey().getQualifiedName()));

    this.valueToIdMapOfRemoved =
        createExpiringCache(
            removedSubscriptionsExpireAfterAccessSeconds,
            ticker,
            notification ->
                logger.info(
                    "Removing expired subscription {} id from value->id cache",
                    notification.getValue().getSubscriptionName().getQualifiedName()));

    notificationsBus.registerSubscriptionCallback(this);
  }

  private <K, V> Cache<K, V> createExpiringCache(
      long expireAfterSeconds, Ticker ticker, RemovalListener<K, V> removalListener) {
    CacheBuilder<K, V> cacheBuilder =
        CacheBuilder.newBuilder()
            .expireAfterAccess(expireAfterSeconds, TimeUnit.SECONDS)
            .ticker(ticker)
            .removalListener(removalListener);
    return cacheBuilder.build();
  }

  @Override
  public void start() {
    subscriptionsCache.listActiveSubscriptionNames().forEach(this::putSubscriptionId);
  }

  private void putSubscriptionId(SubscriptionName name) {
    SubscriptionId id = subscriptionIdProvider.getSubscriptionId(name);
    nameToIdMap.put(name, id);
    valueToIdMap.put(id.getValue(), id);
  }

  @Override
  public Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName) {
    return Optional.ofNullable(
        Optional.ofNullable(nameToIdMap.get(subscriptionName))
            .orElseGet(() -> nameToIdMapOfRemoved.getIfPresent(subscriptionName)));
  }

  @Override
  public Optional<SubscriptionId> getSubscriptionId(long id) {
    return Optional.ofNullable(
        Optional.ofNullable(valueToIdMap.get(id))
            .orElseGet(() -> valueToIdMapOfRemoved.getIfPresent(id)));
  }

  @Override
  public void onSubscriptionCreated(Subscription subscription) {
    putSubscriptionId(subscription.getQualifiedName());
  }

  @Override
  public void onSubscriptionChanged(Subscription subscription) {
    putSubscriptionId(subscription.getQualifiedName());
  }

  @Override
  public void onSubscriptionRemoved(Subscription subscription) {
    Optional.ofNullable(nameToIdMap.remove(subscription.getQualifiedName()))
        .ifPresent(
            id -> {
              valueToIdMap.remove(id.getValue());

              nameToIdMapOfRemoved.put(subscription.getQualifiedName(), id);
              valueToIdMapOfRemoved.put(id.getValue(), id);
            });
  }
}
