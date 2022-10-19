package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterResult;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class FilteringMessageReceiver implements MessageReceiver {
    private final MessageReceiver receiver;
    private final FilteredMessageHandler filteredMessageHandler;
    private final FilterChainFactory filterChainFactory;

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
        this.filterChain = filterChainFactory.create(subscription.getFilters());
    }

    @Override
    public Optional<Message> next() {
        return receiver.next().map(message ->
                allow(message) ? message : null
        );
    }

    private boolean allow(Message message) {
        FilterResult result = filterChain.apply(message);
        filteredMessageHandler.handle(result, message, subscription);
        return !result.isFiltered();
    }

    @Override
    public void stop() {
        receiver.stop();
    }

    @Override
    public void update(Subscription newSubscription) {
        if (!Objects.equals(subscription.getFilters(), newSubscription.getFilters())) {
            this.filterChain = filterChainFactory.create(newSubscription.getFilters());
        }
        this.subscription = newSubscription;
        this.receiver.update(newSubscription);
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        receiver.commit(offsets);
    }

    @Override
    public boolean moveOffset(PartitionOffset offset) {
        return receiver.moveOffset(offset);
    }
}
