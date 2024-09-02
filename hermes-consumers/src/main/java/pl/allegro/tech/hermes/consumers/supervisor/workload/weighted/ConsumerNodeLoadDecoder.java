package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ConsumerLoadDecoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ConsumerLoadDecoder.SubscriptionsDecoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderDecoder;

class ConsumerNodeLoadDecoder {

  private static final Logger logger = getLogger(ConsumerNodeLoadDecoder.class);

  private final SubscriptionIds subscriptionIds;

  ConsumerNodeLoadDecoder(SubscriptionIds subscriptionIds) {
    this.subscriptionIds = subscriptionIds;
  }

  ConsumerNodeLoad decode(byte[] bytes) {
    MessageHeaderDecoder header = new MessageHeaderDecoder();
    ConsumerLoadDecoder body = new ConsumerLoadDecoder();

    UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    header.wrap(buffer, 0);

    if (header.schemaId() != ConsumerLoadDecoder.SCHEMA_ID
        || header.templateId() != ConsumerLoadDecoder.TEMPLATE_ID) {
      logger.warn(
          "Unable to decode consumer node load, schema or template id mismatch. "
              + "Required by decoder: [schema id={}, template id={}], "
              + "encoded in payload: [schema id={}, template id={}]",
          ConsumerLoadDecoder.SCHEMA_ID,
          ConsumerLoadDecoder.TEMPLATE_ID,
          header.schemaId(),
          header.templateId());
      return ConsumerNodeLoad.UNDEFINED;
    }

    body.wrap(buffer, header.encodedLength(), header.blockLength(), header.version());

    return new ConsumerNodeLoad(body.cpuUtilization(), decodeSubscriptionLoads(body));
  }

  private Map<SubscriptionName, SubscriptionLoad> decodeSubscriptionLoads(
      ConsumerLoadDecoder body) {
    Map<SubscriptionName, SubscriptionLoad> subscriptionLoads = new HashMap<>();
    for (SubscriptionsDecoder loadPerSubscriptionDecoder : body.subscriptions()) {
      long id = loadPerSubscriptionDecoder.id();
      Optional<SubscriptionId> subscriptionId = subscriptionIds.getSubscriptionId(id);
      if (subscriptionId.isPresent()) {
        double operationsPerSecond = loadPerSubscriptionDecoder.operationsPerSecond();
        SubscriptionLoad load = new SubscriptionLoad(operationsPerSecond);
        subscriptionLoads.put(subscriptionId.get().getSubscriptionName(), load);
      }
    }
    return subscriptionLoads;
  }
}
