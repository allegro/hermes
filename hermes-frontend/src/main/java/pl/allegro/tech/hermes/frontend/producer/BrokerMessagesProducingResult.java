package pl.allegro.tech.hermes.frontend.producer;

public enum BrokerMessagesProducingResult {

    SUCCESS, FAILURE;

    boolean isFailure() {
        return this == FAILURE;
    }
}
