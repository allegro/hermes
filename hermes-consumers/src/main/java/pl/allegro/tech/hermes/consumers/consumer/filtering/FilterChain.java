package pl.allegro.tech.hermes.consumers.consumer.filtering;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.List;

public class FilterChain {

    public FilterChain(final List<MessageFilter> messageFilters) {
        this.messageFilters = messageFilters;
    }

    private List<MessageFilter> messageFilters;

    public FilterResult apply(final Message message) {

        for (MessageFilter filter : messageFilters) {
            if (!filter.test(message)) {
                return FilterResult.failed(filter.type());
            }
        }

        return FilterResult.PASS;
    }
}
