package pl.allegro.tech.hermes.frontend.producer;

import java.util.Collection;

public final class BrokerMessagesBatchProducingResults {

    private final Collection<BrokerMessagesProducingResult> brokerMessagesProducingResults;

    public BrokerMessagesBatchProducingResults(Collection<BrokerMessagesProducingResult> brokerMessagesProducingResults) {
        this.brokerMessagesProducingResults = brokerMessagesProducingResults;
    }

    public boolean isFailure() {
        return this.brokerMessagesProducingResults.stream().anyMatch(BrokerMessagesProducingResult::isFailure);
    }

    @Override
    public String toString() {
        return String.format(
                "failed:%s, success:%s",
                brokerMessagesProducingResults.stream().filter(BrokerMessagesProducingResult::isFailure).count(),
                brokerMessagesProducingResults.stream().filter(it -> !it.isFailure()).count()
        );
    }
}
