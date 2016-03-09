package pl.allegro.tech.hermes.consumers.consumer.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

@NotThreadSafe
public class MessageBatchReceiver {
    private static final Logger logger = LoggerFactory.getLogger(MessageBatchReceiver.class);

    private final MessageReceiver receiver;
    private final MessageBatchFactory batchFactory;
    private final MessageConverterResolver messageConverterResolver;
    private final MessageContentWrapper messageContentWrapper;
    private final HermesMetrics hermesMetrics;
    private final Trackers trackers;
    private final Queue<Message> inflight;
    private final Topic topic;
    private boolean receiving = true;


    public MessageBatchReceiver(MessageReceiver receiver,
                                MessageBatchFactory batchFactory,
                                HermesMetrics hermesMetrics,
                                MessageConverterResolver messageConverterResolver,
                                MessageContentWrapper messageContentWrapper,
                                Topic topic,
                                Trackers trackers) {
        this.receiver = receiver;
        this.batchFactory = batchFactory;
        this.hermesMetrics = hermesMetrics;
        this.messageConverterResolver = messageConverterResolver;
        this.messageContentWrapper = messageContentWrapper;
        this.topic = topic;
        this.trackers = trackers;
        this.inflight = new ArrayDeque<>(1);
    }

    public MessageBatchingResult next(Subscription subscription) {
        logger.debug("Trying to allocate memory for new batch [subscription={}]", subscription.getId());
        MessageBatch batch = batchFactory.createBatch(subscription);
        logger.debug("New batch allocated [subscription={}]", subscription.getId());
        List<MessageMetadata> discarded = new ArrayList<>();
        while (isReceiving() && !batch.isReadyForDelivery()) {
            try {
                Message message = inflight.isEmpty() ? receive(subscription, batch.getId()) : inflight.poll();

                if (batch.canFit(message.getData())) {
                    batch.append(message.getData(), messageMetadata(subscription, batch.getId(), message));
                } else if (batch.isBiggerThanTotalCapacity(message.getData())) {
                    logger.error("Message size exceeds buffer total capacity [size={}, capacity={}, subscription={}]",
                            message.getData().length, batch.getCapacity(), subscription.getId());
                    discarded.add(toMessageMetadata(message, subscription));
                } else {
                    logger.info("Message too large for current batch [message_size={}, subscription={}]", message.getData().length, subscription.getId());
                    checkArgument(inflight.offer(message));
                    break;
                }
            } catch (MessageReceivingTimeoutException ex) {
                // ignore
            }
        }
        logger.debug("Batch is ready for delivery [subscription={}]", subscription.getId());
        return new MessageBatchingResult(batch.close(), discarded);
    }

    private Message receive(Subscription subscription, String batchId) {
        Message next = receiver.next();
        next = messageConverterResolver.converterFor(next, subscription).convert(next, topic);
        next = message().fromMessage(next).withData(wrap(subscription, next)).build();
        hermesMetrics.incrementInflightCounter(subscription);
        trackers.get(subscription).logInflight(messageMetadata(subscription, batchId, next));
        return next;
    }

    private byte[] wrap(Subscription subscription, Message next) {
        return messageContentWrapper.wrap(next.getData(), next.getId(), next.getPublishingTimestamp(), topic, subscription.getContentType(), next.getExternalMetadata());
    }

    private MessageMetadata messageMetadata(Subscription subscription, String batchId, Message next) {
        return new MessageMetadata(next.getId(), batchId, next.getOffset(), next.getPartition(),
                subscription.getQualifiedTopicName(), subscription.getName(),
                next.getPublishingTimestamp(), next.getReadingTimestamp());
    }

    private boolean isReceiving() {
        return receiving;
    }

    public void stop() {
        receiving = false;
    }
}
