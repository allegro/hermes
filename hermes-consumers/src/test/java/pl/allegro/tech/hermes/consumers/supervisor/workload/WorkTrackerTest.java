package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class WorkTrackerTest extends ZookeeperBaseTest {

    private final String basePath = "/hermes/consumers/runtime";
    private final String supervisorId = "c1";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);

    private final  WorkTracker workTracker = new WorkTracker(zookeeperClient, new ObjectMapper(), basePath, supervisorId,
            executorService, subscriptionRepository);

    @Before
    public void before() throws Exception {
        workTracker.start(new ArrayList<>());
    }

    @After
    public void cleanup() throws Exception {
        workTracker.stop();
        deleteAllNodes();
    }

    @Test
    public void shouldForceAssignment() {
        // given
        Subscription sub = anySubscription();

        // when
        forceAssignment(sub);

        // then
        assertThat(workTracker.isAssignedTo(sub.toSubscriptionName(), supervisorId)).isTrue();
    }

    @Test
    public void shouldDropAssignment() {
        // given
        Subscription sub = forceAssignment(anySubscription());

        // when
        workTracker.dropAssignment(sub);
        wait.untilZookeeperPathIsEmpty(basePath, sub.toSubscriptionName().toString());

        // then
        assertThat(workTracker.isAssignedTo(sub.toSubscriptionName(), supervisorId)).isFalse();
    }

    @Test
    public void shouldReturnAllAssignments() {
        // given
        Subscription s1 = forceAssignment(anySubscription());
        Subscription s2 = forceAssignment(anySubscription());

        // when
        SubscriptionAssignmentView assignments = workTracker.getAssignments();

        // then
        assertThat(assignments.getSubscriptions()).containsOnly(s1.toSubscriptionName(), s2.toSubscriptionName());
        assertThat(workTracker.isAssignedTo(s1.toSubscriptionName(), supervisorId)).isTrue();
        assertThat(workTracker.isAssignedTo(s2.toSubscriptionName(), supervisorId)).isTrue();
    }

    @Test
    public void shouldApplyAssignmentChangesCreatingNewNodesInZookeeper() {
        // given
        Subscription s1 = anySubscription();
        SubscriptionAssignmentView view = stateWithSingleAssignment(s1);

        // when
        workTracker.apply(view);

        // then
        wait.untilZookeeperPathIsCreated(basePath, s1.toSubscriptionName().toString(), supervisorId);
    }

    @Test
    public void shouldApplyAssignmentChangesByRemovingInvalidNodes() {
        // given
        Subscription s1 = forceAssignment(anySubscription());

        // when
        workTracker.apply(stateWithNoAssignments());

        // then
        wait.untilZookeeperPathNotExists(basePath, s1.toSubscriptionName().toString(), supervisorId);
    }

    @Test
    public void shouldApplyAssignmentChangesByRemovingInvalidSubscriptionNode() {
        // given
        Subscription s1 = dropAssignment(forceAssignment(anySubscription()));

        // when
        workTracker.apply(stateWithNoAssignments());

        // then
        wait.untilZookeeperPathNotExists(basePath, s1.toSubscriptionName().toString());
    }

    @Test
    public void shouldApplyAssignmentChangesByRemovingSubscriptionNode() {
        // given
        Subscription s1 = anySubscription();
        SubscriptionAssignmentView view = stateWithSingleAssignment(s1);
        workTracker.apply(view);
        wait.untilZookeeperPathIsCreated(basePath, s1.toSubscriptionName().toString(), supervisorId);

        // when
        workTracker.apply(stateWithNoAssignments());

        // then
        wait.untilZookeeperPathNotExists(basePath, s1.toSubscriptionName().toString());
    }

    @Test
    public void shouldApplyAssignmentChangesByAddingNewNodes() {
        // given
        Subscription s1 = forceAssignment(anySubscription());
        Subscription s2 = forceAssignment(anySubscription());
        SubscriptionAssignmentView view = new SubscriptionAssignmentView(
                ImmutableMap.of(
                        s1.toSubscriptionName(), ImmutableSet.of(assignment(supervisorId, s1.toSubscriptionName()), assignment("otherConsumer", s1.toSubscriptionName())),
                        s2.toSubscriptionName(), ImmutableSet.of(assignment(supervisorId, s2.toSubscriptionName()))));

        // when
        workTracker.apply(view);

        // then
        wait.untilZookeeperPathIsCreated(basePath, s1.toSubscriptionName().toString(), supervisorId);
        wait.untilZookeeperPathIsCreated(basePath, s1.toSubscriptionName().toString(), "otherConsumer");
        wait.untilZookeeperPathIsCreated(basePath, s2.toSubscriptionName().toString(), supervisorId);
    }

    private SubscriptionAssignmentView stateWithSingleAssignment(Subscription subscription) {
        return new SubscriptionAssignmentView(ImmutableMap.of(subscription.toSubscriptionName(),
                ImmutableSet.of(assignment(supervisorId, subscription.toSubscriptionName()))));
    }

    private SubscriptionAssignmentView stateWithNoAssignments() {
        return new SubscriptionAssignmentView(Collections.emptyMap());
    }

    private SubscriptionAssignment assignment(String supervisorId, SubscriptionName subscriptionName) {
        return new SubscriptionAssignment(supervisorId, subscriptionName);
    }

    private Subscription anySubscription() {
        SubscriptionName name = SubscriptionName.fromString("com.test.topic$" + Math.abs(UUID.randomUUID().getMostSignificantBits()));
        Subscription subscription = subscription(name).build();
        given(subscriptionRepository.getSubscriptionDetails(name)).willReturn(subscription);
        return subscription;
    }

    private Subscription forceAssignment(Subscription sub) {
        workTracker.forceAssignment(sub);
        wait.untilZookeeperPathIsCreated(basePath, sub.toSubscriptionName().toString(), supervisorId);
        return sub;
    }

    private Subscription dropAssignment(Subscription sub) {
        workTracker.dropAssignment(sub);
        wait.untilZookeeperPathNotExists(basePath, sub.toSubscriptionName().toString(), supervisorId);
        return sub;
    }
}
