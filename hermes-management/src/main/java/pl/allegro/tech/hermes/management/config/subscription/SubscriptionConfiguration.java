package pl.allegro.tech.hermes.management.config.subscription;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.config.CacheProperties;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionOwnerCache;
import pl.allegro.tech.hermes.management.domain.subscription.validator.EndpointAddressFormatValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.EndpointAddressValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.EndpointOwnershipValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.NoOpEndpointOwnershipValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriberWithAccessToAnyTopic;
import pl.allegro.tech.hermes.management.domain.subscription.validator.SubscriptionValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;

@Configuration
@EnableConfigurationProperties({SubscriptionProperties.class, CacheProperties.class})
public class SubscriptionConfiguration {

  @Bean
  public SubscriptionOwnerCache subscriptionOwnerCache(
      SubscriptionRepository subscriptionRepository, CacheProperties cacheProperties) {
    return new SubscriptionOwnerCache(
        subscriptionRepository, cacheProperties.getSubscriptionOwnerRefreshRateInSeconds());
  }

  @Bean
  public SubscriptionValidator subscriptionValidator(
      OwnerIdValidator ownerIdValidator,
      ApiPreconditions apiPreconditions,
      TopicManagement topicManagement,
      SubscriptionRepository subscriptionRepository,
      List<EndpointAddressValidator> endpointAddressValidators,
      EndpointOwnershipValidator endpointOwnershipValidator,
      SubscriptionProperties subscriptionProperties) {
    return new SubscriptionValidator(
        ownerIdValidator,
        apiPreconditions,
        topicManagement,
        subscriptionRepository,
        endpointAddressValidators,
        endpointOwnershipValidator,
        createListOfSubscribersWithAccessToAnyTopic(subscriptionProperties));
  }

  private List<SubscriberWithAccessToAnyTopic> createListOfSubscribersWithAccessToAnyTopic(
      SubscriptionProperties subscriptionProperties) {
    return subscriptionProperties.getSubscribersWithAccessToAnyTopic().stream()
        .map(
            subscriber ->
                new SubscriberWithAccessToAnyTopic(
                    subscriber.getOwnerSource(),
                    subscriber.getOwnerId(),
                    subscriber.getProtocols()))
        .collect(toList());
  }

  @Bean
  public EndpointOwnershipValidator defaultEndpointOwnershipValidator() {
    return new NoOpEndpointOwnershipValidator();
  }

  @Bean
  public EndpointAddressValidator endpointAddressFormatValidator(
      SubscriptionProperties subscriptionProperties) {
    return new EndpointAddressFormatValidator(
        subscriptionProperties.getAdditionalEndpointProtocols());
  }
}
