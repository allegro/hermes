package pl.allegro.tech.hermes.management.domain.search;

import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedItem;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedSubscriptionItem;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedTopicItem;

public class SearchPredicateFactory {
  private static final Logger logger = LoggerFactory.getLogger(SearchPredicateFactory.class);

  public Predicate<CachedItem> buildPredicate(SearchQuery query) {
    return filterItemPredicate(query);
  }

  private Predicate<CachedItem> filterItemPredicate(SearchQuery query) {
    Predicate<CachedTopicItem> topicPredicate = filterTopic(query);
    Predicate<CachedSubscriptionItem> subscriptionPredicate = filterSubscription(query);

    return item -> {
      if (item instanceof CachedTopicItem topicItem) {
        return topicPredicate.test(topicItem);
      } else if (item instanceof CachedSubscriptionItem subscriptionItem) {
        return subscriptionPredicate.test(subscriptionItem);
      } else {
        logger.warn("Unknown CachedItem type: {}", item.getClass().getName());
        return false;
      }
    };
  }

  private Predicate<CachedTopicItem> filterTopic(SearchQuery query) {
    return item ->
        containsIgnoreCase(item.name(), query.query())
            || containsIgnoreCase(item.topic().getOwner().getId(), query.query());
  }

  private Predicate<CachedSubscriptionItem> filterSubscription(SearchQuery query) {
    return item ->
        containsIgnoreCase(item.name(), query.query())
            || containsIgnoreCase(item.subscription().getOwner().getId(), query.query())
            || containsIgnoreCase(item.subscription().getEndpoint().getEndpoint(), query.query());
  }

  private boolean containsIgnoreCase(String source, String query) {
    return source.toLowerCase().contains(query.toLowerCase());
  }
}
