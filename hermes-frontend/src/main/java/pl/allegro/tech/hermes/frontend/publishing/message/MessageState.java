package pl.allegro.tech.hermes.frontend.publishing.message;

import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.ERROR_IN_SENDING_TO_KAFKA;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.FULLY_READ;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.INIT;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.PREMATURE_TIMEOUT;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.READING;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.READING_ERROR;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.READING_TIMEOUT;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENDING_TO_KAFKA;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENDING_TO_KAFKA_PRODUCER_QUEUE;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENT_TO_KAFKA;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.TIMEOUT_SENDING_TO_KAFKA;

import java.util.concurrent.atomic.AtomicReference;

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
    TIMEOUT_SENDING_TO_KAFKA,
  }

  private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

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
    return state.compareAndSet(SENDING_TO_KAFKA, SENT_TO_KAFKA)
        || state.compareAndSet(SENDING_TO_KAFKA_PRODUCER_QUEUE, SENT_TO_KAFKA);
  }

  public boolean setTimeoutSendingToKafka() {
    return state.compareAndSet(SENDING_TO_KAFKA_PRODUCER_QUEUE, TIMEOUT_SENDING_TO_KAFKA)
        || state.compareAndSet(SENDING_TO_KAFKA, TIMEOUT_SENDING_TO_KAFKA);
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
}
