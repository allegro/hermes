package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveWorkBalancer;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class SelectiveWorkBalancerTest {

    private static final int CONSUMERS_PER_SUBSCRIPTION = 2;
    private static final int MAX_SUBSCRIPTIONS_PER_CONSUMER = 2;

    private SelectiveWorkBalancer workBalancer = new SelectiveWorkBalancer();

    @Test
    public void shouldPerformSubscriptionsCleanup() {
        // given
        List<SubscriptionName> subscriptions = someSubscriptions(1);
        List<String> supervisors = someSupervisors(1);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

        // when
        WorkBalancingResult target = workBalancer.balance(emptyList(), supervisors, currentState, constraints);

        // then
        assertThat(target.getAssignmentsView().getSubscriptions()).isEmpty();
        assertThat(currentState.deletions(target.getAssignmentsView()).getAllAssignments()).hasSize(subscriptions.size());
    }

    @Test
    public void shouldPerformSupervisorsCleanup() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(1);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

        // when
        supervisors.remove(1);
        WorkBalancingResult work = workBalancer.balance(subscriptions, supervisors, currentState, constraints);

        // then
        assertThat(currentState.deletions(work.getAssignmentsView()).getAllAssignments()).hasSize(1);
        assertThatSubscriptionIsAssignedTo(work.getAssignmentsView(), subscriptions.get(0), supervisors.get(0));
    }

    @Test
    public void shouldBalanceWorkForSingleSubscription() {
        // given
        List<String> supervisors = someSupervisors(1);
        List<SubscriptionName> subscriptions = someSubscriptions(1);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors, constraints);

        // then
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(0), supervisors.get(0));
    }

    @Test
    public void shouldBalanceWorkForMultipleConsumersAndSingleSubscription() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(1);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors, constraints);

        // then
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(0), supervisors);
    }

    @Test
    public void shouldBalanceWorkForMultipleConsumersAndMultipleSubscriptions() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(2);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors, constraints);

        // then
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(0), supervisors);
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(1), supervisors);
    }

    @Test
    public void shouldNotOverloadConsumers() {
        // given
        List<String> supervisors = someSupervisors(1);
        List<SubscriptionName> subscriptions = someSubscriptions(3);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors, constraints);

        // then
        assertThat(view.getAssignmentsForConsumerNode(supervisors.get(0))).hasSize(2);
    }

    @Test
    public void shouldRebalanceAfterConsumerDisappearing() {
        // given
        List<String> supervisors = ImmutableList.of("c1", "c2");
        List<SubscriptionName> subscriptions = someSubscriptions(2);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

        // when
        List<String> extendedSupervisorsList = ImmutableList.of("c1", "c3");
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, extendedSupervisorsList, currentState, constraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getSubscriptionsForConsumerNode("c3")).containsOnly(subscriptions.get(0), subscriptions.get(1));
    }

    @Test
    public void shouldAssignWorkToNewConsumersByWorkStealing() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(2);
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                CONSUMERS_PER_SUBSCRIPTION,
                MAX_SUBSCRIPTIONS_PER_CONSUMER,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

        // when
        supervisors.add("new-supervisor");
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, supervisors, currentState, constraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsForConsumerNode("new-supervisor").size()).isGreaterThan(0);
        assertThat(stateAfterRebalance.getAssignmentsForSubscription(subscriptions.get(0))).hasSize(2);
        assertThat(stateAfterRebalance.getAssignmentsForSubscription(subscriptions.get(1))).hasSize(2);
    }

    @Test
    public void shouldEquallyAssignWorkToConsumers() {
        // given
        List<String> supervisors = ImmutableList.of("c1", "c2");
        List<SubscriptionName> subscriptions = someSubscriptions(50);
        SelectiveWorkBalancer workBalancer = new SelectiveWorkBalancer();
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                2,
                200,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = workBalancer.balance(
                subscriptions,
                supervisors,
                new SubscriptionAssignmentView(emptyMap()),
                constraints
        ).getAssignmentsView();

        // when
        List<String> extendedSupervisorsList = ImmutableList.of("c1", "c2", "c3");
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, extendedSupervisorsList, currentState, constraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsForConsumerNode("c3")).hasSize(50 * 2 / 3);
    }

    @Test
    public void shouldReassignWorkToFreeConsumers() {
        // given
        List<String> supervisors = ImmutableList.of("c1");
        List<SubscriptionName> subscriptions = someSubscriptions(10);
        SelectiveWorkBalancer workBalancer = new SelectiveWorkBalancer();
        WorkloadConstraints constraints = new WorkloadConstraints(
                supervisors.size(),
                1,
                100,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = workBalancer.balance(
                subscriptions,
                supervisors,
                new SubscriptionAssignmentView(emptyMap()),
                constraints
        ).getAssignmentsView();

        // when
        ImmutableList<String> extendedSupervisorsList = ImmutableList.of("c1", "c2", "c3", "c4", "c5");
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, extendedSupervisorsList, currentState, constraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsForConsumerNode("c5")).hasSize(2);
    }

    @Test
    public void shouldRemoveRedundantWorkAssignmentsToKeepWorkloadMinimal() {
        // given
        List<String> supervisors = ImmutableList.of("c1", "c2", "c3");
        List<SubscriptionName> subscriptions = someSubscriptions(10);
        SelectiveWorkBalancer workBalancer = new SelectiveWorkBalancer();
        WorkloadConstraints initialConstraints = new WorkloadConstraints(
                supervisors.size(),
                3,
                100,
                emptyMap(),
                emptyMap()
        );
        SubscriptionAssignmentView currentState = workBalancer.balance(
                subscriptions,
                supervisors,
                new SubscriptionAssignmentView(emptyMap()),
                initialConstraints
        ).getAssignmentsView();

        // when
        WorkloadConstraints newConstraints = new WorkloadConstraints(
                supervisors.size(),
                3,
                100,
                ImmutableMap.of(subscriptions.get(0), new Constraints(1)),
                emptyMap()
        );
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, supervisors, currentState, newConstraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsCountForSubscription(subscriptions.get(0))).isEqualTo(1);
    }

    @DataProvider
    public static Object[][] subscriptionConstraints() {
        return new Object[][] {
                { 1 }, { 3 }
        };
    }

    @Test
    @UseDataProvider("subscriptionConstraints")
    public void shouldAssignConsumersForSubscriptionsAccordingToConstraints(int requiredConsumersNumber) {
        // given
        SubscriptionAssignmentView initialState = new SubscriptionAssignmentView(emptyMap());

        List<String> supervisors = ImmutableList.of("c1", "c2", "c3");
        List<SubscriptionName> subscriptions = someSubscriptions(4);
        SelectiveWorkBalancer workBalancer = new SelectiveWorkBalancer();
        WorkloadConstraints subscriptionConstraints = new WorkloadConstraints(
                supervisors.size(),
                2,
                4,
                ImmutableMap.of(subscriptions.get(0), new Constraints(requiredConsumersNumber)),
                emptyMap()
        );

        // when
        SubscriptionAssignmentView state = workBalancer
                .balance(subscriptions, supervisors, initialState, subscriptionConstraints)
                .getAssignmentsView();

        // then
        assertThat(state.getAssignmentsForSubscription(subscriptions.get(0)).size()).isEqualTo(requiredConsumersNumber);
        assertThat(state.getAssignmentsForSubscription(subscriptions.get(1)).size()).isEqualTo(2);
        assertThat(state.getAssignmentsForSubscription(subscriptions.get(2)).size()).isEqualTo(2);
        assertThat(state.getAssignmentsForSubscription(subscriptions.get(3)).size()).isEqualTo(2);
    }

    private SubscriptionAssignmentView initialState(List<SubscriptionName> subscriptions, List<String> supervisors,
                                                    WorkloadConstraints workloadConstraints) {
        return workBalancer.balance(subscriptions, supervisors, new SubscriptionAssignmentView(emptyMap()), workloadConstraints)
                .getAssignmentsView();
    }

    private List<SubscriptionName> someSubscriptions(int count) {
        return IntStream.range(0, count).mapToObj(i -> anySubscription()).collect(toList());
    }

    private List<String> someSupervisors(int count) {
        return IntStream.range(0, count).mapToObj(i -> "c" + i).collect(toList());
    }

    private SubscriptionName anySubscription() {
        return SubscriptionName.fromString("tech.topic$s" + UUID.randomUUID().getMostSignificantBits());
    }

    private void assertThatSubscriptionIsAssignedTo(SubscriptionAssignmentView work, SubscriptionName sub, String... nodeIds) {
        assertThatSubscriptionIsAssignedTo(work, sub, asList(nodeIds));
    }

    private void assertThatSubscriptionIsAssignedTo(SubscriptionAssignmentView work, SubscriptionName sub, List<String> nodeIds) {
        assertThat(work.getAssignmentsForSubscription(sub))
                .extracting(SubscriptionAssignment::getConsumerNodeId)
                .containsOnly(nodeIds.toArray(String[]::new));
    }
}
