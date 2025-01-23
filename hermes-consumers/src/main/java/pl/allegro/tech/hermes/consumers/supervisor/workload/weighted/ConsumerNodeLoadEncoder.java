package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ConsumerLoadEncoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ConsumerLoadEncoder.SubscriptionsEncoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderEncoder;

class ConsumerNodeLoadEncoder {

  private final SubscriptionIds subscriptionIds;
  private final MutableDirectBuffer buffer;

  ConsumerNodeLoadEncoder(SubscriptionIds subscriptionIds, int bufferSize) {
    this.subscriptionIds = subscriptionIds;
    this.buffer = new ExpandableDirectByteBuffer(bufferSize);
  }

  byte[] encode(ConsumerNodeLoad consumerNodeLoad) {
    Map<SubscriptionId, SubscriptionLoad> subscriptionLoads =
        mapToSubscriptionIds(consumerNodeLoad);

    MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    ConsumerLoadEncoder body =
        new ConsumerLoadEncoder().wrapAndApplyHeader(buffer, 0, headerEncoder);

    SubscriptionsEncoder loadPerSubscriptionEncoder =
        body.cpuUtilization(consumerNodeLoad.getCpuUtilization())
            .subscriptionsCount(subscriptionLoads.size());

    for (Map.Entry<SubscriptionId, SubscriptionLoad> entry : subscriptionLoads.entrySet()) {
      SubscriptionId subscriptionId = entry.getKey();
      SubscriptionLoad load = entry.getValue();
      loadPerSubscriptionEncoder
          .next()
          .id(subscriptionId.getValue())
          .operationsPerSecond(load.getOperationsPerSecond());
    }

    int len = headerEncoder.encodedLength() + body.encodedLength();

    byte[] dst = new byte[len];
    buffer.getBytes(0, dst);
    return dst;
  }

  private Map<SubscriptionId, SubscriptionLoad> mapToSubscriptionIds(ConsumerNodeLoad metrics) {
    Map<SubscriptionId, SubscriptionLoad> subscriptionLoads = new HashMap<>();
    for (Map.Entry<SubscriptionName, SubscriptionLoad> entry :
        metrics.getLoadPerSubscription().entrySet()) {
      Optional<SubscriptionId> subscriptionId = subscriptionIds.getSubscriptionId(entry.getKey());
      subscriptionId.ifPresent(id -> subscriptionLoads.put(id, entry.getValue()));
    }
    return subscriptionLoads;
  }
}
