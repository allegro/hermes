package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import com.google.common.collect.Sets;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.SubscriptionConstraints;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.WorkloadConstraints;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;

public class AvailableWork extends Spliterators.AbstractSpliterator<SubscriptionAssignment> {
    private SubscriptionAssignmentView state;
    private final int consumersPerSubscription;
    private final int maxSubscriptionsPerConsumer;
    private final WorkloadConstraints constraints;

    private AvailableWork(SubscriptionAssignmentView state, int consumersPerSubscription, int maxSubscriptionsPerConsumer, WorkloadConstraints constraints) {
        super(Long.MAX_VALUE, 0);
        this.state = state;
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.constraints = constraints;
    }

    @Override
    public boolean tryAdvance(Consumer<? super SubscriptionAssignment> action) {
        Set<String> availableConsumers = availableConsumerNodes(state);
        if (!availableConsumers.isEmpty()) {
            Optional<SubscriptionAssignment> subscriptionAssignment = getNextSubscription(state, availableConsumers)
                    .map(subscription -> getNextSubscriptionAssignment(state, availableConsumers, subscription));
            if (subscriptionAssignment.isPresent()) {
                action.accept(subscriptionAssignment.get());
                return true;
            }
        }
        return false;
    }

    private Optional<SubscriptionName> getNextSubscription(SubscriptionAssignmentView state, Set<String> availableConsumerNodes) {
        return state.getSubscriptions().stream()
                .filter(s -> state.getAssignmentsCountForSubscription(s) < getConsumersPerSubscription(s))
                .filter(s -> !Sets.difference(availableConsumerNodes, state.getConsumerNodesForSubscription(s)).isEmpty())
                .min(Comparator.comparingInt(state::getAssignmentsCountForSubscription));
    }

    private SubscriptionAssignment getNextSubscriptionAssignment(SubscriptionAssignmentView state,
                                                                 Set<String> availableConsumerNodes,
                                                                 SubscriptionName subscriptionName) {
        return availableConsumerNodes.stream()
                .filter(s -> !state.getSubscriptionsForConsumerNode(s).contains(subscriptionName))
                .min(Comparator.comparingInt(state::getAssignmentsCountForConsumerNode))
                .map(s -> new SubscriptionAssignment(s, subscriptionName))
                .get();
    }

    private Set<String> availableConsumerNodes(SubscriptionAssignmentView state) {
        return state.getConsumerNodes().stream()
                .filter(s -> state.getAssignmentsCountForConsumerNode(s) < maxSubscriptionsPerConsumer)
                .filter(s -> state.getAssignmentsCountForConsumerNode(s) < state.getSubscriptionsCount())
                .collect(toSet());
    }

    private int getConsumersPerSubscription(SubscriptionName subscriptionName) {
        if (constraints == null) {
            return consumersPerSubscription;
        }

        final SubscriptionConstraints subscriptionConstraints = constraints.getSubscriptionConstraints(subscriptionName);
        if (subscriptionConstraints != null) {
            return subscriptionConstraints.getRequiredConsumersNumber();
        }
        return consumersPerSubscription;
    }

    public static Stream<SubscriptionAssignment> stream(SubscriptionAssignmentView state,
                                                        int consumersPerSubscription,
                                                        int maxSubscriptionsPerConsumer,
                                                        WorkloadConstraints constraints) {
        AvailableWork work = new AvailableWork(state, consumersPerSubscription, maxSubscriptionsPerConsumer, constraints);
        return StreamSupport.stream(work, false);
    }
}
