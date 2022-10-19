package pl.allegro.tech.hermes.consumers.consumer.batch;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

@NotThreadSafe
public class MessageBatchReceiver {

    private static final Logger logger = LoggerFactory.getLogger(MessageBatchReceiver.class);

    private final MessageReceiver receiver;
    private final MessageBatchFactory batchFactory;
    private final MessageConverterResolver messageConverterResolver;
    private final CompositeMessageContentWrapper compositeMessageContentWrapper;
    private final SubscriptionMetrics metrics;
    private final Trackers trackers;
    private final Queue<Message> inflight;
    private final Topic topic;
    private final SubscriptionLoadRecorder loadRecorder;
    private boolean receiving = true;

    public MessageBatchReceiver(MessageReceiver receiver,
                                MessageBatchFactory batchFactory,
                                SubscriptionMetrics metrics,
                                MessageConverterResolver messageConverterResolver,
                                CompositeMessageContentWrapper compositeMessageContentWrapper,
                                Topic topic,
                                Trackers trackers,
                                SubscriptionLoadRecorder loadRecorder) {
        this.receiver = receiver;
        this.batchFactory = batchFactory;
        this.metrics = metrics;
        this.messageConverterResolver = messageConverterResolver;
        this.compositeMessageContentWrapper = compositeMessageContentWrapper;
        this.topic = topic;
        this.trackers = trackers;
        this.loadRecorder = loadRecorder;
        this.inflight = new ArrayDeque<>(1);
    }

    public MessageBatchingResult next(Subscription subscription, Runnable signalsInterrupt) {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to allocate memory for new batch [subscription={}]", subscription.getQualifiedName());
        }

        MessageBatch batch = batchFactory.createBatch(subscription);
        if (logger.isDebugEnabled()) {
            logger.debug("New batch allocated [subscription={}]", subscription.getQualifiedName());
        }
        List<MessageMetadata> discarded = new ArrayList<>();

        while (isReceiving() && !batch.isReadyForDelivery() && !Thread.currentThread().isInterrupted()) {
            loadRecorder.recordSingleOperation();
            signalsInterrupt.run();
            Optional<Message> maybeMessage = inflight.isEmpty()
                    ? readAndTransform(subscription, batch.getId())
                    : Optional.ofNullable(inflight.poll());

            if (maybeMessage.isPresent()) {
                Message message = maybeMessage.get();

                if (batch.canFit(message.getData())) {
                    batch.append(message.getData(), toMessageMetadata(message, subscription, batch.getId()));
                } else if (batch.isBiggerThanTotalCapacity(message.getData())) {
                    logger.error("Message size exceeds buffer total capacity [size={}, capacity={}, subscription={}]",
                            message.getData().length, batch.getCapacity(), subscription.getQualifiedName());
                    discarded.add(toMessageMetadata(message, subscription));
                } else {
                    logger.debug(
                            "Message too large for current batch [message_size={}, subscription={}]",
                            message.getData().length, subscription.getQualifiedName()
                    );
                    checkArgument(inflight.offer(message));
                    break;
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Batch is ready for delivery [subscription={}]", subscription.getQualifiedName());
        }
        return new MessageBatchingResult(batch.close(), discarded);
    }

    private Optional<Message> readAndTransform(Subscription subscription, String batchId) {
        Optional<Message> maybeMessage = receiver.next();

        if (maybeMessage.isPresent()) {
            Message message = maybeMessage.get();

            Message transformed = messageConverterResolver.converterFor(message, subscription).convert(message, topic);
            transformed = message().fromMessage(transformed).withData(wrap(subscription, transformed)).build();
            metrics.markAttempt();
            trackers.get(subscription).logInflight(toMessageMetadata(transformed, subscription, batchId));

            return Optional.of(transformed);
        }
        return Optional.empty();
    }

    private byte[] wrap(Subscription subscription, Message next) {
        switch (subscription.getContentType()) {
            case AVRO:
                return compositeMessageContentWrapper.wrapAvro(next.getData(), next.getId(), next.getPublishingTimestamp(), topic,
                        next.<Schema>getSchema().get(), next.getExternalMetadata());
            case JSON:
                return compositeMessageContentWrapper.wrapJson(next.getData(), next.getId(), next.getPublishingTimestamp(),
                        next.getExternalMetadata());
            default:
                throw new UnsupportedContentTypeException(subscription);
        }
    }

    private boolean isReceiving() {
        return receiving;
    }

    public void stop() {
        receiving = false;
        receiver.stop();
    }

    public void updateSubscription(Subscription modifiedSubscription) {
        receiver.update(modifiedSubscription);
    }

    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        receiver.commit(offsets);
    }

    public boolean moveOffset(PartitionOffset offset) {
        return receiver.moveOffset(offset);
    }
}
