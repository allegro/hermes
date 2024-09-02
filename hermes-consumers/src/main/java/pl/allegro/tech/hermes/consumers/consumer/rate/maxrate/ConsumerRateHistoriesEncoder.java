package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.List;
import java.util.Map;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.MessageHeaderEncoder;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.RateHistoryEncoder;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;

class ConsumerRateHistoriesEncoder {

  private final MutableDirectBuffer buffer;
  private final SubscriptionIds subscriptionIds;

  ConsumerRateHistoriesEncoder(SubscriptionIds subscriptionIds, int bufferSize) {
    this.subscriptionIds = subscriptionIds;
    this.buffer = new ExpandableDirectByteBuffer(bufferSize);
  }

  byte[] encode(ConsumerRateHistory consumerRateHistory) {
    MessageHeaderEncoder header = new MessageHeaderEncoder();
    RateHistoryEncoder body = new RateHistoryEncoder();

    Map<SubscriptionId, RateHistory> historiesFiltered =
        consumerRateHistory.toSubscriptionIdsMap(subscriptionIds::getSubscriptionId);

    RateHistoryEncoder.SubscriptionsEncoder subscriptionsEncoder =
        body.wrapAndApplyHeader(buffer, 0, header).subscriptionsCount(historiesFiltered.size());

    historiesFiltered.forEach(
        (id, rateHistory) -> {
          List<Double> rates = rateHistory.getRates();
          RateHistoryEncoder.SubscriptionsEncoder.RatesEncoder ratesEncoder =
              subscriptionsEncoder.next().id(id.getValue()).ratesCount(rates.size());

          rates.forEach(rate -> ratesEncoder.next().rate(rate));
        });
    int len = header.encodedLength() + body.encodedLength();

    byte[] dst = new byte[len];
    buffer.getBytes(0, dst);
    return dst;
  }
}
