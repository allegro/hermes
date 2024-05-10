package pl.allegro.tech.hermes.consumers.consumer.offset

import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter

class MockMessageCommitter implements MessageCommitter {

    private final List<OffsetsToCommit> recordedValues = []

    private int iteration = -1

    boolean nothingCommitted(int iteration) {
        return wereCommitted(iteration)
    }

    boolean wereCommitted(int iteration, SubscriptionPartitionOffset... expectedOffsets) {
        OffsetsToCommit offsetsToCommit = recordedValues[iteration - 1]
        Set<SubscriptionPartitionOffset> allOffsets = [] as Set
        offsetsToCommit.each { subscription ->
            allOffsets.addAll(offsetsToCommit.offsets)
        }

        Set<SubscriptionPartitionOffset> expectedOffsetsSet = [] as Set
        expectedOffsets.each { expectedOffsetsSet.add(it) }

        allOffsets == expectedOffsetsSet
    }

    @Override
    void commitOffsets(OffsetsToCommit offsetsToCommit) {
        recordedValues.add(offsetsToCommit)
        iteration++
    }
}
