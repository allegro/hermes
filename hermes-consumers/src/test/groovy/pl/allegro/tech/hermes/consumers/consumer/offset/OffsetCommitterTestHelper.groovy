package pl.allegro.tech.hermes.consumers.consumer.offset

class OffsetCommitterTestHelper {

    private final List<Set<SubscriptionPartitionOffset>> recordedValues = []

    private int iteration = -1

    boolean nothingCommitted(int iteration) {
        return wereCommitted(iteration)
    }

    boolean wereCommitted(int iteration, SubscriptionPartitionOffset... expectedOffsets) {
        Set<SubscriptionPartitionOffset> allOffsets = [] as Set
        recordedValues[iteration - 1].each {allOffsets.add(it)}

        Set<SubscriptionPartitionOffset> expectedOffsetsSet = [] as Set
        expectedOffsets.each { expectedOffsetsSet.add(it) }

        allOffsets == expectedOffsetsSet
    }

    void markCommittedOffsets(Set<SubscriptionPartitionOffset> subscriptionPartitionOffsets) {
        recordedValues.add(subscriptionPartitionOffsets)
        iteration++
    }
}
