package pl.allegro.tech.hermes.management.domain.subscription;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

@Component
public class SubscriptionOwnerCache {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionOwnerCache.class);

  private final SubscriptionRepository subscriptionRepository;
  private final ScheduledExecutorService scheduledExecutorService;

  private Multimap<OwnerId, SubscriptionName> cache =
      Multimaps.synchronizedMultimap(ArrayListMultimap.create());

  public SubscriptionOwnerCache(
      SubscriptionRepository subscriptionRepository,
      @Value("${subscriptionOwnerCache.refreshRateInSeconds}") int refreshRateInSeconds) {
    this.subscriptionRepository = subscriptionRepository;
    scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("subscription-owner-cache-%d").build());
    scheduledExecutorService.scheduleAtFixedRate(
        this::refillCache, 0, refreshRateInSeconds, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  Collection<SubscriptionName> get(OwnerId ownerId) {
    return cache.get(ownerId);
  }

  Collection<SubscriptionName> getAll() {
    return cache.values();
  }

  void onRemovedSubscription(String subscriptionName, TopicName topicName) {
    cache
        .entries()
        .removeIf(
            entry -> entry.getValue().equals(new SubscriptionName(subscriptionName, topicName)));
  }

  void onCreatedSubscription(Subscription subscription) {
    cache.put(subscription.getOwner(), subscription.getQualifiedName());
  }

  void onUpdatedSubscription(Subscription oldSubscription, Subscription newSubscription) {
    cache.remove(oldSubscription.getOwner(), oldSubscription.getQualifiedName());
    cache.put(newSubscription.getOwner(), newSubscription.getQualifiedName());
  }

  private void refillCache() {
    try {
      logger.info("Starting filling SubscriptionOwnerCache");
      long start = System.currentTimeMillis();
      Multimap<OwnerId, SubscriptionName> cache = ArrayListMultimap.create();
      subscriptionRepository
          .listAllSubscriptions()
          .forEach(
              subscription -> cache.put(subscription.getOwner(), subscription.getQualifiedName()));
      this.cache = Multimaps.synchronizedMultimap(cache);
      long end = System.currentTimeMillis();
      logger.info("SubscriptionOwnerCache filled. Took {}ms", end - start);
    } catch (Exception e) {
      logger.error("Error while filling SubscriptionOwnerCache", e);
    }
  }
}
