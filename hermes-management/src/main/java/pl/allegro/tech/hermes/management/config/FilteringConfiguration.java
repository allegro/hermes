package pl.allegro.tech.hermes.management.config;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.filtering.MessageFilters;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.filtering.header.HeaderSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.json.JsonPathSubscriptionMessageFilterCompiler;

@Configuration
public class FilteringConfiguration {

  @Bean
  FilterChainFactory filterChainFactory() {
    List<SubscriptionMessageFilterCompiler> subscriptionFilterCompilers =
        Arrays.asList(
            new AvroPathSubscriptionMessageFilterCompiler(),
            new JsonPathSubscriptionMessageFilterCompiler(),
            new HeaderSubscriptionMessageFilterCompiler());
    MessageFilters messageFilters = new MessageFilters(emptyList(), subscriptionFilterCompilers);
    return new FilterChainFactory(messageFilters);
  }
}
