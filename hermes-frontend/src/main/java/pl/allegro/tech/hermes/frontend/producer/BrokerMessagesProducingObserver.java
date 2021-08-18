package pl.allegro.tech.hermes.frontend.producer;

public interface BrokerMessagesProducingObserver {

    void notifyAboutBrokerMessageProducingResult(BrokerMessagesProducingResult brokerMessagesProducingResult);

    BrokerMessagesBatchProducingResults waitForMessagesBatchProducingResults();
}
