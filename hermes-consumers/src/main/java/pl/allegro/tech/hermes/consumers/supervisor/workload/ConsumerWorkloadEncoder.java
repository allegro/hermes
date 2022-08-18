package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.AssignmentsEncoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderEncoder;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class ConsumerWorkloadEncoder {

    private final SubscriptionIds subscriptionIds;
    private final MutableDirectBuffer buffer;

    ConsumerWorkloadEncoder(SubscriptionIds subscriptionIds, int bufferSize) {
        this.subscriptionIds = subscriptionIds;
        this.buffer = new ExpandableDirectByteBuffer(bufferSize);
    }

    byte[] encode(Collection<SubscriptionName> subscriptions) {
        MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        AssignmentsEncoder body = new AssignmentsEncoder();

        Set<SubscriptionId> ids = subscriptions.stream()
                .map(this.subscriptionIds::getSubscriptionId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        AssignmentsEncoder.SubscriptionsEncoder subscriptionsEncoder = body.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .subscriptionsCount(ids.size());
        ids.forEach(id -> subscriptionsEncoder.next()
                .id(id.getValue()));

        int len = headerEncoder.encodedLength() + body.encodedLength();

        byte[] dst = new byte[len];
        buffer.getBytes(0, dst);
        return dst;
    }
}
