package pl.allegro.tech.hermes.frontend.publishing.message;

import java.util.concurrent.atomic.AtomicReference;

import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.*;

public class MessageState {

    enum State {
        INIT,
        PREMATURE_TIMEOUT,
        READING,
        READING_TIMEOUT,
        READING_ERROR,
        FULLY_READ,
        SENDING_TO_KAFKA_PRODUCER_QUEUE,
        ERROR_IN_SENDING_TO_KAFKA,
        SENDING_TO_KAFKA,
        SENT_TO_KAFKA,
        DELAYED_SENDING,
        DELAYED_PROCESSING
    }

    private volatile boolean timeoutHasPassed = false;
    private AtomicReference<State> state = new AtomicReference<>(State.INIT);

    public boolean setReading() {
        return state.compareAndSet(INIT, READING);
    }

    public void setPrematureTimeout() {
        state.compareAndSet(INIT, PREMATURE_TIMEOUT);
    }

    public boolean setFullyRead() {
        return state.compareAndSet(READING, FULLY_READ);
    }

    public boolean isReadingTimeout() {
        return state.get() == READING_TIMEOUT;
    }

    public void setSendingToKafkaProducerQueue() {
        state.set(SENDING_TO_KAFKA_PRODUCER_QUEUE);
    }

    public boolean setSentToKafka() {
        return state.compareAndSet(SENDING_TO_KAFKA, SENT_TO_KAFKA) || state.compareAndSet(SENDING_TO_KAFKA_PRODUCER_QUEUE, SENT_TO_KAFKA);
    }

    public boolean isDelayed() {
        return timeoutHasPassed || state.get() == DELAYED_SENDING || state.get() == DELAYED_PROCESSING;
    }

    public boolean setDelayedSending() {
        return state.compareAndSet(SENDING_TO_KAFKA, DELAYED_SENDING);
    }

    public boolean setReadingTimeout() {
        return state.compareAndSet(READING, READING_TIMEOUT);
    }

    public boolean setReadingError() {
        return state.compareAndSet(READING, READING_ERROR);
    }

    public void setErrorInSendingToKafka() {
        state.set(ERROR_IN_SENDING_TO_KAFKA);
    }

    public boolean setSendingToKafka() {
        return state.compareAndSet(SENDING_TO_KAFKA_PRODUCER_QUEUE, SENDING_TO_KAFKA);
    }

    public boolean setDelayedProcessing() {
        return timeoutHasPassed && state.compareAndSet(SENDING_TO_KAFKA, DELAYED_PROCESSING);
    }

    public void setTimeoutHasPassed() {
        this.timeoutHasPassed = true;
    }
}
