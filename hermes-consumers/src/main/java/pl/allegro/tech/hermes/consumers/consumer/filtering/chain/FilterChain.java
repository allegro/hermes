package pl.allegro.tech.hermes.consumers.consumer.filtering.chain;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.MessageFilter;

import java.util.ArrayList;
import java.util.List;

public final class FilterChain {
    private final List<MessageFilter> messageFilters;

    public FilterChain(final List<MessageFilter> messageFilters) {
        this.messageFilters = new ArrayList<>(messageFilters);
    }

    public FilterResult apply(final Message message) {
        for (MessageFilter filter : messageFilters) {
            try {
                if (!filter.test(message)) {
                    return FilterResult.failed(filter.getType(), "logical");
                }
            } catch (Exception ex) {
                return FilterResult.failed(filter.getType(), ex);
            }
        }
        return FilterResult.PASS;
    }
}
