package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

import java.util.Objects;

public class FilteringMessageReceiver implements MessageReceiver {
    private MessageReceiver receiver;
    private FilteredMessageHandler filteredMessageHandler;
    private FilterChainFactory filterChainFactory;

    private volatile FilterChain filterChain;
    private Subscription subscription;
    private boolean consuming = true;

    public FilteringMessageReceiver(MessageReceiver receiver,
                                    FilteredMessageHandler filteredMessageHandler,
                                    FilterChainFactory filterChainFactory,
                                    Subscription subscription) {
        this.receiver = receiver;
        this.filteredMessageHandler = filteredMessageHandler;
        this.filterChainFactory = filterChainFactory;
        this.subscription = subscription;
        this.filterChain = filterChainFactory.create(subscription);
    }

    @Override
    public Message next() {
        Message message;
        do {
            message = receiver.next();
        } while (consuming && filter(message));
        return message;
    }

    private boolean filter(Message message) {
        FilterResult result = filterChain.apply(message);
        filteredMessageHandler.handle(result, message, subscription);
        return result.isFiltered();
    }

    @Override
    public void stop() {
        this.consuming = false;
        receiver.stop();
    }

    @Override
    public void update(Subscription newSubscription) {
        if (!Objects.equals(subscription.getFilters(), newSubscription.getFilters())) {
            this.filterChain = filterChainFactory.create(newSubscription);
        }
        this.subscription = newSubscription;
    }
}
