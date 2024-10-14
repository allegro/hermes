package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Map;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.MaxRateEncoder;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.MessageHeaderEncoder;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;

class ConsumerMaxRatesEncoder {

  private final SubscriptionIds subscriptionIds;
  private final MutableDirectBuffer buffer;

  ConsumerMaxRatesEncoder(SubscriptionIds subscriptionIds, int bufferSize) {
    this.subscriptionIds = subscriptionIds;
    this.buffer = new ExpandableDirectByteBuffer(bufferSize);
  }

  byte[] encode(ConsumerMaxRates consumerMaxRates) {
    MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    MaxRateEncoder body = new MaxRateEncoder();

    Map<SubscriptionId, MaxRate> filteredRates =
        consumerMaxRates.toSubscriptionsIdsMap(subscriptionIds::getSubscriptionId);

    MaxRateEncoder.SubscriptionsEncoder subscriptionsEncoder =
        body.wrapAndApplyHeader(buffer, 0, headerEncoder).subscriptionsCount(filteredRates.size());

    filteredRates.forEach(
        (id, maxRate) -> {
          subscriptionsEncoder.next().id(id.getValue()).maxRate(maxRate.getMaxRate());
        });

    int len = headerEncoder.encodedLength() + body.encodedLength();

    byte[] dst = new byte[len];
    buffer.getBytes(0, dst);
    return dst;
  }
}
