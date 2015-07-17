package pl.allegro.tech.hermes.frontend.publishing.message;

import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENDING_TO_KAFKA;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.SENT_TO_KAFKA;

public class MessageState {

    public enum State {
        WAITING_FOR_FIRST_PORTION_OF_DATA,
        PARSING,
        PARSED,
        SENDING_TO_KAFKA_PRODUCER_QUEUE,
        SENDING_TO_KAFKA,
        SENT_TO_KAFKA
    }

    private volatile State state = State.WAITING_FOR_FIRST_PORTION_OF_DATA;

    public MessageState() {
    }

    public synchronized void setState(State state) {
        this.state = state;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized boolean wasDelegatedToKafka() {
        return state == SENDING_TO_KAFKA || state == SENT_TO_KAFKA;
    }
}
