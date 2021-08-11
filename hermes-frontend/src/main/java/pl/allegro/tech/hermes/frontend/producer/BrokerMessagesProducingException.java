package pl.allegro.tech.hermes.frontend.producer;

public class BrokerMessagesProducingException extends RuntimeException {

    public BrokerMessagesProducingException(String message, Exception cause) {
        super(message, cause);
    }

    public BrokerMessagesProducingException(String message) {
        super(message);
    }
}
