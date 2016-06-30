package pl.allegro.tech.hermes.frontend.publishing.message;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.*;

public class MessageState {

    enum State {
        READING,
        READING_TIMEOUT,
        FULLY_READ,
        SENDING_TO_KAFKA_PRODUCER_QUEUE,
        ERROR_IN_SENDING_TO_KAFKA,
        SENDING_TO_KAFKA,
        SENT_TO_KAFKA,
        DELAYED_SENDING,
        DELAYED_PROCESSING
    }

    private volatile boolean delayedProcessing = false;
    private AtomicReference<State> state = new AtomicReference<>(State.READING);

    public void onFullyReadSet(Consumer<Void> consumer) {
        if (state.compareAndSet(READING, FULLY_READ)) {
            consumer.accept(null);
        }
    }

    public void onNotTimeout(Consumer<Void> consumer) {
        if (state.get() != READING_TIMEOUT) {
            consumer.accept(null);
        }
    }

    public void setSendingToKafkaProducerQueue() {
        state.set(SENDING_TO_KAFKA_PRODUCER_QUEUE);
    }

    public MessageState onSentToKafkaSet(Consumer<Void> consumer) {
        if (state.compareAndSet(SENDING_TO_KAFKA, SENT_TO_KAFKA) || state.compareAndSet(SENDING_TO_KAFKA_PRODUCER_QUEUE, SENT_TO_KAFKA)) {
            consumer.accept(null);
        }
        return this;
    }

    public void onDelayed(Consumer<Void> consumer) {
        if (state.get() == DELAYED_SENDING || state.get() == DELAYED_PROCESSING) {
            consumer.accept(null);
        }
    }

    public boolean onDelayedSendingSet(Consumer<Void> consumer) {
        if (state.compareAndSet(SENDING_TO_KAFKA, DELAYED_SENDING)) {
            consumer.accept(null);
            return true;
        }
        return false;
    }

    public boolean onReadingTimeoutSet(Consumer<Void> consumer) {
        if (state.compareAndSet(READING, READING_TIMEOUT)) {
            consumer.accept(null);
            return true;
        }
        return false;
    }

    public void setErrorInSendingToKafka() {
        state.set(ERROR_IN_SENDING_TO_KAFKA);
    }

    public void onSendingToKafkaSet(Consumer<Void> consumer) {
        if (state.compareAndSet(SENDING_TO_KAFKA_PRODUCER_QUEUE, SENDING_TO_KAFKA)) {
            consumer.accept(null);
        }
    }

    public void onDelayedProcessingSet(Consumer<Void> consumer) {
        if (delayedProcessing && state.compareAndSet(SENDING_TO_KAFKA, DELAYED_PROCESSING)) {
            consumer.accept(null);
        }
    }

    public void setDelayedProcessing() {
        this.delayedProcessing = true;
    }
}
