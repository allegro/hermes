package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.filtering.MessageFilters;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.filtering.json.JsonPathSubscriptionMessageFilterCompiler;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Configuration
public class FilteringConfiguration {

    @Bean
    FilterChainFactory filterChainFactory() {
        List<SubscriptionMessageFilterCompiler> subscriptionFilterCompilers = Arrays.asList(
                new AvroPathSubscriptionMessageFilterCompiler(),
                new JsonPathSubscriptionMessageFilterCompiler()
        );
        MessageFilters messageFilters = new MessageFilters(emptyList(), subscriptionFilterCompilers);
        return new FilterChainFactory(messageFilters);
    }
}
