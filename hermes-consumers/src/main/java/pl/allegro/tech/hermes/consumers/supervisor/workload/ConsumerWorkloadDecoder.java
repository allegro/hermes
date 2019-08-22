package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.agrona.concurrent.UnsafeBuffer;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.AssignmentsDecoder;
import pl.allegro.tech.hermes.consumers.supervisor.workload.sbe.stubs.MessageHeaderDecoder;

import java.util.HashSet;
import java.util.Set;

class ConsumerWorkloadDecoder {

    private final SubscriptionIds subscriptionIds;

    ConsumerWorkloadDecoder(SubscriptionIds subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    Set<SubscriptionName> decode(byte[] data) {
        MessageHeaderDecoder header = new MessageHeaderDecoder();
        AssignmentsDecoder body = new AssignmentsDecoder();

        UnsafeBuffer buffer = new UnsafeBuffer(data);
        header.wrap(buffer, 0);

        if (header.templateId() != AssignmentsDecoder.TEMPLATE_ID) {
            throw new IllegalStateException(String.format("AssignmentsDecoder TEMPLATE_ID=%d does not match encoded TEMPLATE_ID=%d",
                    AssignmentsDecoder.TEMPLATE_ID, header.templateId()));
        }
        body.wrap(buffer, header.encodedLength(), header.blockLength(), header.version());

        Set<SubscriptionName> subscriptions = new HashSet<>();
        for (AssignmentsDecoder.SubscriptionsDecoder subscriptionDecoder : body.subscriptions()) {
            long id = subscriptionDecoder.id();
            subscriptionIds.getSubscriptionId(id)
                    .map(SubscriptionId::getSubscriptionName)
                    .ifPresent(subscriptions::add);
        }
        return subscriptions;
    }
}
