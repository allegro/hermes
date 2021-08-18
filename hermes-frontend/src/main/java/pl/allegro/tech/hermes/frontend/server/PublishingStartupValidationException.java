package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesBatchProducingResults;

public class PublishingStartupValidationException extends RuntimeException {

    PublishingStartupValidationException(BrokerMessagesBatchProducingResults results) {
        super(String.format("Error while validating publishing messages, last result: %s", results));
    }
}
