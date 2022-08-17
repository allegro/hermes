package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderEncoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.ProfilesEncoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class SubscriptionProfilesEncoder {

    private final SubscriptionIds subscriptionIds;
    private final MutableDirectBuffer buffer;

    SubscriptionProfilesEncoder(SubscriptionIds subscriptionIds, int bufferSize) {
        this.subscriptionIds = subscriptionIds;
        this.buffer = new ExpandableDirectByteBuffer(bufferSize);
    }

    byte[] encode(Map<SubscriptionName, SubscriptionProfile> profiles) {
        Map<SubscriptionId, SubscriptionProfile> subscriptionProfiles = mapToSubscriptionIds(profiles);

        MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        ProfilesEncoder body = new ProfilesEncoder()
                .wrapAndApplyHeader(buffer, 0, headerEncoder);

        ProfilesEncoder.SubscriptionsEncoder subscriptionsEncoder = body
                .subscriptionsCount(subscriptionProfiles.size());

        for (Map.Entry<SubscriptionId, SubscriptionProfile> entry : subscriptionProfiles.entrySet()) {
            SubscriptionId subscriptionId = entry.getKey();
            SubscriptionProfile profile = entry.getValue();
            subscriptionsEncoder.next()
                    .id(subscriptionId.getValue())
                    .operationsPerSecond(profile.getWeight().getOperationsPerSecond())
                    .lastRebalanceTimestamp(toMillis(profile.getLastRebalanceTimestamp()));
        }

        int len = headerEncoder.encodedLength() + body.encodedLength();

        byte[] dst = new byte[len];
        buffer.getBytes(0, dst);
        return dst;
    }

    private Map<SubscriptionId, SubscriptionProfile> mapToSubscriptionIds(Map<SubscriptionName, SubscriptionProfile> profiles) {
        Map<SubscriptionId, SubscriptionProfile> subscriptionProfiles = new HashMap<>();
        for (Map.Entry<SubscriptionName, SubscriptionProfile> entry : profiles.entrySet()) {
            Optional<SubscriptionId> subscriptionId = subscriptionIds.getSubscriptionId(entry.getKey());
            subscriptionId.ifPresent(id -> subscriptionProfiles.put(id, entry.getValue()));
        }
        return subscriptionProfiles;
    }

    private long toMillis(Instant timestamp) {
        if (timestamp == null) {
            return -1;
        }
        return timestamp.toEpochMilli();
    }
}
