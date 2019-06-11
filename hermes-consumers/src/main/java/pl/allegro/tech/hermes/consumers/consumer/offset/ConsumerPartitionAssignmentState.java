package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

public class ConsumerPartitionAssignmentState {

    private static final Logger logger = getLogger(ConsumerPartitionAssignmentState.class);

    Map<SubscriptionName, Set<Integer>> assigned = new ConcurrentHashMap<>();

    Map<SubscriptionName, Long> terms = new ConcurrentHashMap<>();

    public void assign(SubscriptionName name, Collection<Integer> partitions) {
        incrementTerm(name);
        logger.info("Assigning partitions {} to {}, term={}", partitions, name, currentTerm(name));
        assigned.compute(name, ((subscriptionName, assigned) -> {
            HashSet<Integer> extended = new HashSet<>(partitions);
            if (assigned == null) {
                return extended;
            } else {
                extended.addAll(assigned);
                return extended;
            }
        }));
    }

    public void revoke(SubscriptionName name, Collection<Integer> partitions) {
        logger.info("Revoking partitions {} from {}", partitions, name);
        assigned.computeIfPresent(name, (subscriptionName, assigned) -> {
            Set<Integer> filtered = assigned.stream().filter(p -> !partitions.contains(p)).collect(toSet());
            return filtered.isEmpty() ? null : filtered;
        });
    }

    public void revokeAll(SubscriptionName name) {
        logger.info("Revoking all partitions from {}", name);
        assigned.remove(name);
    }

    private void incrementTerm(SubscriptionName name) {
        terms.compute(name, ((subscriptionName, term) -> term == null ? 0L : term + 1L));
    }

    public long currentTerm(SubscriptionName name) {
        return terms.getOrDefault(name, -1L);
    }

    public boolean isAssignedPartitionAtCurrentTerm(SubscriptionPartition subscriptionPartition) {
        return currentTerm(subscriptionPartition.getSubscriptionName()) == subscriptionPartition.getPartitionAssignmentTerm()
                && isAssigned(subscriptionPartition.getSubscriptionName(), subscriptionPartition.getPartition());
    }

    private boolean isAssigned(SubscriptionName name, int partition) {
        return assigned.containsKey(name) && assigned.get(name).contains(partition);
    }
}
