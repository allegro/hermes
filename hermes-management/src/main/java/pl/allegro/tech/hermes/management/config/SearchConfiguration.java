package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.search.SearchPredicateFactory;
import pl.allegro.tech.hermes.management.domain.search.SearchService;
import pl.allegro.tech.hermes.management.domain.search.cache.NotificationBasedSearchCache;
import pl.allegro.tech.hermes.management.domain.search.cache.SearchCache;

@Configuration
public class SearchConfiguration {

  @Bean
  public SearchCache searchCache(
      InternalNotificationsBus notificationsBus,
      TopicRepository topicRepository,
      SubscriptionRepository subscriptionRepository) {
    return new NotificationBasedSearchCache(
        notificationsBus, topicRepository, subscriptionRepository);
  }

  @Bean
  public SearchPredicateFactory searchPredicateFactory() {
    return new SearchPredicateFactory();
  }

  @Bean
  public SearchService searchService(
      SearchCache searchCache, SearchPredicateFactory searchPredicateFactory) {
    return new SearchService(searchCache, searchPredicateFactory);
  }
}
