package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.AssignmentsDecoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderDecoder;

class ConsumerWorkloadDecoder {

  private static final Logger logger = getLogger(ConsumerWorkloadDecoder.class);

  private final SubscriptionIds subscriptionIds;

  ConsumerWorkloadDecoder(SubscriptionIds subscriptionIds) {
    this.subscriptionIds = subscriptionIds;
  }

  Set<SubscriptionName> decode(byte[] data) {
    MessageHeaderDecoder header = new MessageHeaderDecoder();
    AssignmentsDecoder body = new AssignmentsDecoder();

    UnsafeBuffer buffer = new UnsafeBuffer(data);
    header.wrap(buffer, 0);

    if (header.schemaId() != AssignmentsDecoder.SCHEMA_ID
        || header.templateId() != AssignmentsDecoder.TEMPLATE_ID) {
      logger.warn(
          "Unable to decode assignments, schema or template id mismatch. "
              + "Required by decoder: [schema id={}, template id={}], "
              + "encoded in payload: [schema id={}, template id={}]",
          AssignmentsDecoder.SCHEMA_ID,
          AssignmentsDecoder.TEMPLATE_ID,
          header.schemaId(),
          header.templateId());
      return Collections.emptySet();
    }
    body.wrap(buffer, header.encodedLength(), header.blockLength(), header.version());

    Set<SubscriptionName> subscriptions = new HashSet<>();
    for (AssignmentsDecoder.SubscriptionsDecoder subscriptionDecoder : body.subscriptions()) {
      long id = subscriptionDecoder.id();
      subscriptionIds
          .getSubscriptionId(id)
          .map(SubscriptionId::getSubscriptionName)
          .ifPresent(subscriptions::add);
    }
    return subscriptions;
  }
}
