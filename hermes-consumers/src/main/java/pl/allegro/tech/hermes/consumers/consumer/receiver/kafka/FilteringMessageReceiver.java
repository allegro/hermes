package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;

public class FilteringMessageReceiver implements MessageReceiver {
    private MessageReceiver receiver;
    private FilteredMessageHandler filteredMessageHandler;
    private FilterChainFactory filterChainFactory;

    private volatile FilterChain filterChain;
    private Subscription subscription;

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
    public Optional<Message> next() {
        return receiver.next().map(message ->
                allow(message) ? message : null
        );
    }

    private boolean allow(Message message) {
        FilterResult result = filterChain.apply(message);
        filteredMessageHandler.handle(result, message, subscription, (offset) -> receiver.commit(singleton(offset)));
        return !result.isFiltered();
    }

    @Override
    public void stop() {
        receiver.stop();
    }

    @Override
    public void update(Subscription newSubscription) {
        if (!Objects.equals(subscription.getFilters(), newSubscription.getFilters())) {
            this.filterChain = filterChainFactory.create(newSubscription);
        }
        this.subscription = newSubscription;
        this.receiver.update(newSubscription);
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        receiver.commit(offsets);
    }

    @Override
    public void moveOffset(SubscriptionPartitionOffset offset) {
        receiver.moveOffset(offset);
    }
}
