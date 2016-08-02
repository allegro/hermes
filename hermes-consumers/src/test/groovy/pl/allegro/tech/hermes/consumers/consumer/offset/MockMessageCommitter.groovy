package pl.allegro.tech.hermes.consumers.consumer.offset

import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter

class MockMessageCommitter implements MessageCommitter {

    private final List<FailedToCommitOffsets> returnedValues = []

    private final List<OffsetsToCommit> recordedValues = []

    private int iteration = -1

    boolean wereCommitted(int iteration, SubscriptionPartitionOffset... expectedOffsets) {
        OffsetsToCommit offsetsToCommit = recordedValues[iteration - 1]
        Set<SubscriptionPartitionOffset> allOffsets = [] as Set
        offsetsToCommit.subscriptionNames().each { subscription ->
            allOffsets.addAll(offsetsToCommit.batchFor(subscription))
        }

        Set<SubscriptionPartitionOffset> expectedOffsetsSet = [] as Set
        expectedOffsets.each { expectedOffsetsSet.add(it) }

        allOffsets == expectedOffsetsSet
    }

    void returnValue(FailedToCommitOffsets failedOffsets) {
        returnedValues.add(failedOffsets)
    }

    @Override
    FailedToCommitOffsets commitOffsets(OffsetsToCommit offsetsToCommit) {
        recordedValues.add(offsetsToCommit)
        iteration++
        return returnedValues.size() > iteration ? returnedValues[iteration] : new FailedToCommitOffsets()
    }

    @Override
    void removeOffset(TopicName topicName, String subscriptionName, KafkaTopicName topic, int partition) throws Exception {

    }
}
