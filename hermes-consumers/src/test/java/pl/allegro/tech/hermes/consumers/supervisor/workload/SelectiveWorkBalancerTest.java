package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.ListAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.SubscriptionConstraints;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.WorkloadConstraints;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveWorkBalancer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.WorkBalancingResult;

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

    private static int CONSUMERS_PER_SUBSCRIPTION = 2;
    private static int MAX_SUBSCRIPTIONS_PER_CONSUMER = 2;

    private WorkloadConstraints defaultConstraints = WorkloadConstraints.defaultConstraints(CONSUMERS_PER_SUBSCRIPTION, MAX_SUBSCRIPTIONS_PER_CONSUMER);
    private SelectiveWorkBalancer workBalancer = new SelectiveWorkBalancer();

    @Test
    public void shouldPerformSubscriptionsCleanup() {
        // given
        List<SubscriptionName> subscriptions = someSubscriptions(1);
        List<String> supervisors = someSupervisors(1);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors);

        // when
        WorkBalancingResult target = workBalancer.balance(emptyList(), supervisors, currentState, defaultConstraints);

        // then
        assertThat(target.getAssignmentsView().getSubscriptions()).isEmpty();
        assertThat(target.getRemovedSubscriptionsCount()).isEqualTo(subscriptions.size());
    }

    @Test
    public void shouldPerformSupervisorsCleanup() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(1);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors);

        // when
        supervisors.remove(1);
        WorkBalancingResult work = workBalancer.balance(subscriptions, supervisors, currentState, defaultConstraints);

        // then
        assertThat(work.getRemovedSupervisorsCount()).isEqualTo(1);
        assertThatSubscriptionIsAssignedTo(work.getAssignmentsView(), subscriptions.get(0), supervisors.get(0));
    }

    @Test
    public void shouldBalanceWorkForSingleSubscription() {
        // given
        List<String> supervisors = someSupervisors(1);
        List<SubscriptionName> subscriptions = someSubscriptions(1);

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors);

        // then
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(0), supervisors.get(0));
    }

    @Test
    public void shouldBalanceWorkForMultipleConsumersAndSingleSubscription() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(1);

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors);

        // then
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(0), supervisors);
    }

    @Test
    public void shouldBalanceWorkForMultipleConsumersAndMultipleSubscriptions() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(2);

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors);

        // then
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(0), supervisors);
        assertThatSubscriptionIsAssignedTo(view, subscriptions.get(1), supervisors);
    }

    @Test
    public void shouldNotOverloadConsumers() {
        // given
        List<String> supervisors = someSupervisors(1);
        List<SubscriptionName> subscriptions = someSubscriptions(3);

        // when
        SubscriptionAssignmentView view = initialState(subscriptions, supervisors);

        // then
        assertThat(view.getAssignmentsForConsumerNode(supervisors.get(0))).hasSize(2);
    }

    @Test
    public void shouldRebalanceAfterConsumerDisappearing() {
        // given
        List<String> supervisors = ImmutableList.of("c1", "c2");
        List<SubscriptionName> subscriptions = someSubscriptions(2);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors);

        // when
        List<String> extendedSupervisorsList = ImmutableList.of("c1", "c3");
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, extendedSupervisorsList, currentState, defaultConstraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getSubscriptionsForConsumerNode("c3")).containsOnly(subscriptions.get(0), subscriptions.get(1));
    }

    @Test
    public void shouldAssignWorkToNewConsumersByWorkStealing() {
        // given
        List<String> supervisors = someSupervisors(2);
        List<SubscriptionName> subscriptions = someSubscriptions(2);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors);

        // when
        supervisors.add("new-supervisor");
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, supervisors, currentState, defaultConstraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsForConsumerNode("new-supervisor").size()).isGreaterThan(0);
        assertThat(stateAfterRebalance.getAssignmentsForSubscription(subscriptions.get(0))).hasSize(2);
        assertThat(stateAfterRebalance.getAssignmentsForSubscription(subscriptions.get(1))).hasSize(2);
    }

    @Test
    public void shouldEquallyAssignWorkToConsumers() {
        // given
        WorkloadConstraints constraints = WorkloadConstraints.defaultConstraints(2, 200);
        List<String> supervisors = ImmutableList.of("c1", "c2");
        List<SubscriptionName> subscriptions = someSubscriptions(50);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

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
        WorkloadConstraints constraints = WorkloadConstraints.defaultConstraints(1, 100);
        List<String> supervisors = ImmutableList.of("c1");
        List<SubscriptionName> subscriptions = someSubscriptions(10);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

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
        WorkloadConstraints constraints = WorkloadConstraints.defaultConstraints(3, 100);
        List<String> supervisors = ImmutableList.of("c1", "c2", "c3");
        List<SubscriptionName> subscriptions = someSubscriptions(10);
        SubscriptionAssignmentView currentState = initialState(subscriptions, supervisors, constraints);

        // when
        WorkloadConstraints newConstraints = WorkloadConstraints.defaultConstraints(1, 100);
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(subscriptions, supervisors, currentState, newConstraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsCountForSubscription(subscriptions.get(0))).isEqualTo(1);
    }

    @Test
    public void shouldNotRemoveAssignmentsThatAreMadeByAdmin() {
        // given
        WorkloadConstraints constraints = WorkloadConstraints.defaultConstraints(1, 100);
        SubscriptionName subscriptionName = SubscriptionName.fromString("a.a$a");
        SubscriptionAssignmentView currentState = initialState(ImmutableList.of(subscriptionName), ImmutableList.of("c1"), constraints)
                .transform((view, transformer) -> {
                    transformer.addAssignment(new SubscriptionAssignment("c1", subscriptionName, true));
                    transformer.addAssignment(new SubscriptionAssignment("c2", subscriptionName, false));
                    transformer.addAssignment(new SubscriptionAssignment("c3", subscriptionName, false));
                    transformer.addAssignment(new SubscriptionAssignment("c4", subscriptionName, false));
                });

        // when
        SubscriptionAssignmentView stateAfterRebalance = workBalancer
                .balance(ImmutableList.of(subscriptionName), ImmutableList.of("c1", "c2", "c3", "c4"), currentState, constraints)
                .getAssignmentsView();

        // then
        assertThat(stateAfterRebalance.getAssignmentsForSubscription(subscriptionName)
                .stream().map(SubscriptionAssignment::getConsumerNodeId).collect(toList())).containsOnly("c2", "c3", "c4");
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
        WorkloadConstraints subscriptionConstraints = new WorkloadConstraints(ImmutableMap.of(
                subscriptions.get(0), new SubscriptionConstraints(subscriptions.get(0), requiredConsumersNumber)
        ), 2, 4);

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

    private SubscriptionAssignmentView initialState(List<SubscriptionName> subscriptions, List<String> supervisors) {
        return initialState(subscriptions, supervisors, defaultConstraints);
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

    private ListAssert<String> assertThatSubscriptionIsAssignedTo(SubscriptionAssignmentView work, SubscriptionName sub, String... nodeIds) {
        return assertThatSubscriptionIsAssignedTo(work, sub, asList(nodeIds));
    }

    private ListAssert<String> assertThatSubscriptionIsAssignedTo(SubscriptionAssignmentView work, SubscriptionName sub, List<String> nodeIds) {
        return assertThat(work.getAssignmentsForSubscription(sub))
                .extracting(SubscriptionAssignment::getConsumerNodeId).containsOnly(nodeIds.stream().toArray(String[]::new));
    }
}
