package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilterChain;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilterChainFactory;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilterResult;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

public class FilteringMessageReceiver implements MessageReceiver {
    private MessageReceiver receiver;
    private FilteredMessageHandler filteredMessageHandler;
    private FilterChainFactory filterChainFactory;

    private volatile boolean dirty = true;
    private Subscription subscription;
    private FilterChain filterChain;
    private boolean consuming = true;

    public FilteringMessageReceiver(MessageReceiver receiver,
                                    FilteredMessageHandler filteredMessageHandler,
                                    FilterChainFactory filterChainFactory, final Subscription subscription) {
        this.receiver = receiver;
        this.filteredMessageHandler = filteredMessageHandler;
        this.filterChainFactory = filterChainFactory;
        this.subscription = subscription;
    }

    private boolean filter(final Message message) {
        FilterResult result = filterChain.apply(message);
        filteredMessageHandler.handle(result, message);
        return result.filtered;
    }

    @Override
    public Message next() {
        Message message;
        do {
            updateFilter();
            message = receiver.next();
        } while (consuming && !filter(message));
        return message;
    }

    private void updateFilter() {
        if (dirty) {
            filterChain = filterChainFactory.create(subscription);
            dirty = false;
        }
    }

    @Override
    public void stop() {
        this.consuming = false;
        receiver.stop();
    }

    @Override
    public void update(final Subscription newSubscription) {
        this.subscription = newSubscription;
        this.dirty = true;
    }
}
