package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.agrona.concurrent.UnsafeBuffer;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.MaxRateDecoder;
import pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs.MessageHeaderDecoder;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;

class ConsumerMaxRatesDecoder {

  private final SubscriptionIds subscriptionIds;

  ConsumerMaxRatesDecoder(SubscriptionIds subscriptionIds) {
    this.subscriptionIds = subscriptionIds;
  }

  ConsumerMaxRates decode(byte[] data) {
    MessageHeaderDecoder header = new MessageHeaderDecoder();
    MaxRateDecoder body = new MaxRateDecoder();

    UnsafeBuffer buffer = new UnsafeBuffer(data);
    header.wrap(buffer, 0);

    if (header.templateId() != MaxRateDecoder.TEMPLATE_ID) {
      throw new IllegalStateException(
          String.format(
              "MaxRatesDecoder TEMPLATE_ID=%d does not match encoded TEMPLATE_ID=%d",
              MaxRateDecoder.TEMPLATE_ID, header.templateId()));
    }
    body.wrap(buffer, header.encodedLength(), header.blockLength(), header.version());

    ConsumerMaxRates result = new ConsumerMaxRates();
    for (MaxRateDecoder.SubscriptionsDecoder subscriptionDecoder : body.subscriptions()) {
      long id = subscriptionDecoder.id();
      double maxRate = subscriptionDecoder.maxRate();
      subscriptionIds
          .getSubscriptionId(id)
          .ifPresent(
              subscriptionId ->
                  result.setMaxRate(subscriptionId.getSubscriptionName(), new MaxRate(maxRate)));
    }
    return result;
  }
}
