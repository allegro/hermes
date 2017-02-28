package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.filtering.MessageFilters;
import pl.allegro.tech.hermes.common.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.common.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.common.filtering.json.JsonPathSubscriptionMessageFilterCompiler;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MessageFiltersConfiguration {

    @Bean
    public MessageFilters messageFilters() {
        List<SubscriptionMessageFilterCompiler> availableFilters = new ArrayList<>();
        availableFilters.add(new JsonPathSubscriptionMessageFilterCompiler());
        availableFilters.add(new AvroPathSubscriptionMessageFilterCompiler());
        return new MessageFilters(new ArrayList<>(), availableFilters);
    }
}
