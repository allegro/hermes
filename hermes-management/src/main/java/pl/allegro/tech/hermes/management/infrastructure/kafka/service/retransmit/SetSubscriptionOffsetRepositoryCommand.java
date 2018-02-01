package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

class SetSubscriptionOffsetRepositoryCommand extends RepositoryCommand<SubscriptionOffsetChangeIndicator> {
    private final TopicName topicName;
    private final String subscriptionName;
    private final String brokersClusterName;
    private final PartitionOffset partitionOffset;

    SetSubscriptionOffsetRepositoryCommand(TopicName topicName, String subscriptionName, String brokersClusterName,
                                           PartitionOffset partitionOffset) {
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
        this.brokersClusterName = brokersClusterName;
        this.partitionOffset = partitionOffset;
    }

    @Override
    public void backup(SubscriptionOffsetChangeIndicator repository) {}

    @Override
    public void execute(SubscriptionOffsetChangeIndicator repository) {
        repository.setSubscriptionOffset(topicName, subscriptionName, brokersClusterName, partitionOffset);
    }

    @Override
    public void rollback(SubscriptionOffsetChangeIndicator repository) {}

    @Override
    public Class<SubscriptionOffsetChangeIndicator> getRepositoryType() {
        return SubscriptionOffsetChangeIndicator.class;
    }

    @Override
    public String toString() {
        return "SetSubscriptionOffset(" + new SubscriptionName(subscriptionName, topicName).getQualifiedName() + ")";
    }
}
