package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResults;

public class PublishingStartupValidationException extends RuntimeException {

    PublishingStartupValidationException(BrokerMessagesProducingResults results) {
        super(String.format("Error while validating publishing messages, last result: %s", results));
    }
}
