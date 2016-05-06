package pl.allegro.tech.hermes.consumers.consumer.filtering.chain;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.filtering.MessageFilter;
import pl.allegro.tech.hermes.consumers.consumer.filtering.MessageFilterSource;

import javax.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class FilterChainFactory {
    private MessageFilterSource availableFilters;

    @Inject
    public FilterChainFactory(MessageFilterSource filters) {
        this.availableFilters = filters;
    }

    public FilterChain create(final Subscription subscription) {
        Stream<MessageFilter> globalFilters = availableFilters.getGlobalFilters().stream();
        Stream<MessageFilter> subscriptionFilters = subscription.getFilters().stream()
                .map(availableFilters::compile);
        return new FilterChain(concat(globalFilters, subscriptionFilters).collect(Collectors.toList()));
    }
}
