package pl.allegro.tech.hermes.common.filtering.chain;

import pl.allegro.tech.hermes.common.filtering.MessageFilter;
import pl.allegro.tech.hermes.common.message.Message;
import pl.allegro.tech.hermes.common.message.MessageContent;

import java.util.ArrayList;
import java.util.List;

public final class FilterChain {
    private final List<MessageFilter> messageFilters;

    public FilterChain(final List<MessageFilter> messageFilters) {
        this.messageFilters = new ArrayList<>(messageFilters);
    }

    public FilterResult apply(final MessageContent message) {
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
