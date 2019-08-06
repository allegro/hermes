package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.agrona.concurrent.UnsafeBuffer;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.MessageHeaderDecoder;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.RateHistoryDecoder;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;

import java.util.ArrayList;
import java.util.List;

class ConsumerRateHistoriesDecoder {

    private final SubscriptionIds subscriptionIds;

    ConsumerRateHistoriesDecoder(SubscriptionIds subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    ConsumerRateHistory decode(byte[] data) {
        MessageHeaderDecoder header = new MessageHeaderDecoder();
        RateHistoryDecoder body = new RateHistoryDecoder();

        UnsafeBuffer buffer = new UnsafeBuffer(data);
        header.wrap(buffer, 0);

        if (header.templateId() != RateHistoryDecoder.TEMPLATE_ID) {
            throw new IllegalStateException(String.format("RateHistoryDecoder TEMPLATE_ID=%d does not match encoded TEMPLATE_ID=%d",
                    RateHistoryDecoder.TEMPLATE_ID, header.templateId()));
        }

        body.wrap(buffer, header.encodedLength(), header.blockLength(), header.version());

        ConsumerRateHistory result = new ConsumerRateHistory();
        for (RateHistoryDecoder.SubscriptionsDecoder subscriptionDecoder : body.subscriptions()) {
            long id = subscriptionDecoder.id();
            List<Double> rates = new ArrayList<>();
            for (RateHistoryDecoder.SubscriptionsDecoder.RatesDecoder ratesDecoder : subscriptionDecoder.rates()) {
                rates.add(ratesDecoder.rate());
            }
            subscriptionIds.getSubscriptionId(id)
                    .ifPresent(subscriptionId -> {
                        result.setRateHistory(subscriptionId.getSubscriptionName(), new RateHistory(rates));
                    });
        }
        return result;
    }
}
