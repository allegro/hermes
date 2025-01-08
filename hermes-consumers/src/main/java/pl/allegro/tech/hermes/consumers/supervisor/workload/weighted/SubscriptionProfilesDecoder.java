package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderDecoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ProfilesDecoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ProfilesDecoder.SubscriptionsDecoder;

class SubscriptionProfilesDecoder {

  private static final Logger logger = getLogger(SubscriptionProfilesDecoder.class);

  private final SubscriptionIds subscriptionIds;

  SubscriptionProfilesDecoder(SubscriptionIds subscriptionIds) {
    this.subscriptionIds = subscriptionIds;
  }

  SubscriptionProfiles decode(byte[] bytes) {
    MessageHeaderDecoder header = new MessageHeaderDecoder();
    ProfilesDecoder body = new ProfilesDecoder();

    UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    header.wrap(buffer, 0);

    if (header.schemaId() != ProfilesDecoder.SCHEMA_ID
        || header.templateId() != ProfilesDecoder.TEMPLATE_ID) {
      logger.warn(
          "Unable to decode subscription profiles, schema or template id mismatch. "
              + "Required by decoder: [schema id={}, template id={}], "
              + "encoded in payload: [schema id={}, template id={}]",
          ProfilesDecoder.SCHEMA_ID,
          ProfilesDecoder.TEMPLATE_ID,
          header.schemaId(),
          header.templateId());
      return SubscriptionProfiles.EMPTY;
    }

    body.wrap(buffer, header.encodedLength(), header.blockLength(), header.version());

    return decodeSubscriptionProfiles(body);
  }

  private SubscriptionProfiles decodeSubscriptionProfiles(ProfilesDecoder body) {
    Instant updateTimestamp = toInstant(body.updateTimestamp());
    Map<SubscriptionName, SubscriptionProfile> subscriptionProfiles = new HashMap<>();
    for (SubscriptionsDecoder subscriptionsDecoder : body.subscriptions()) {
      long id = subscriptionsDecoder.id();
      Optional<SubscriptionId> subscriptionId = subscriptionIds.getSubscriptionId(id);
      if (subscriptionId.isPresent()) {
        double operationsPerSecond = subscriptionsDecoder.operationsPerSecond();
        Instant lastRebalance = toInstant(subscriptionsDecoder.lastRebalanceTimestamp());
        SubscriptionProfile profile =
            new SubscriptionProfile(lastRebalance, new Weight(operationsPerSecond));
        subscriptionProfiles.put(subscriptionId.get().getSubscriptionName(), profile);
      }
    }
    return new SubscriptionProfiles(subscriptionProfiles, updateTimestamp);
  }

  private Instant toInstant(long millis) {
    if (millis < 0) {
      return Instant.MIN;
    }
    return Instant.ofEpochMilli(millis);
  }
}
