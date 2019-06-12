package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.NotificationsBasedSubscriptionCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry.AUTO_ASSIGNED_MARKER;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class WorkTrackerTest extends ZookeeperBaseTest {

    private final String basePath = "/hermes/consumers-workload/primary_dc/runtime";
    private final String supervisorId = "c1";

    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);

    private final ModelAwareZookeeperNotifyingCache notifyingCache = new ModelAwareZookeeperNotifyingCache(
            zookeeperClient, "/hermes", 1
    );

    private final SubscriptionsCache cache = new NotificationsBasedSubscriptionCache(
            new ZookeeperInternalNotificationBus(new ObjectMapper(), notifyingCache),
            groupRepository, topicRepository, subscriptionRepository
    );

    private final SubscriptionAssignmentPathSerializer serializer =
            new SubscriptionAssignmentPathSerializer(basePath, AUTO_ASSIGNED_MARKER);

    private final SubscriptionAssignmentCache subscriptionAssignmentCache =
            new SubscriptionAssignmentCache(zookeeperClient, new MutableConfigFactory(),
                    new ZookeeperPaths("/hermes"), cache);

    private final SubscriptionAssignmentRegistry subscriptionAssignmentRegistry =
            new SubscriptionAssignmentRegistry(zookeeperClient, subscriptionAssignmentCache, serializer);

    private final WorkTracker workTracker = new WorkTracker(supervisorId, subscriptionAssignmentRegistry);

    @Before
    public void before() throws Exception {
        notifyingCache.start();
        subscriptionAssignmentCache.start();
        subscriptionAssignmentRegistry.start();
    }

    @After
    public void cleanup() throws Exception {
        notifyingCache.stop();
        subscriptionAssignmentCache.stop();
        deleteAllNodes();
    }

    @Test
    public void shouldForceAssignment() {
        // given
        Subscription sub = anySubscription();

        // when
        forceAssignment(sub);

        // then
        assertThat(workTracker.isAssignedTo(sub.getQualifiedName(), supervisorId)).isTrue();
    }

    @Test
    public void shouldDropAssignmentAndEmptySubscriptionNode() {
        // given
        Subscription sub = forceAssignment(anySubscription());

        // when
        workTracker.dropAssignment(sub);
        wait.untilZookeeperPathNotExists(basePath, sub.getQualifiedName().toString());

        // then
        assertThat(workTracker.isAssignedTo(sub.getQualifiedName(), supervisorId)).isFalse();
    }

    @Test
    public void shouldReturnAllAssignments() {
        // given
        Subscription s1 = forceAssignment(anySubscription());
        Subscription s2 = forceAssignment(anySubscription());

        // when
        SubscriptionAssignmentView assignments = workTracker.getAssignments();

        // then
        assertThat(assignments.getSubscriptions()).containsOnly(s1.getQualifiedName(), s2.getQualifiedName());
        assertThat(workTracker.isAssignedTo(s1.getQualifiedName(), supervisorId)).isTrue();
        assertThat(workTracker.isAssignedTo(s2.getQualifiedName(), supervisorId)).isTrue();
    }

    @Test
    public void shouldApplyAssignmentChangesCreatingNewNodesInZookeeper() {
        // given
        Subscription s1 = anySubscription();
        SubscriptionAssignmentView view = stateWithSingleAssignment(s1);

        // when
        workTracker.apply(subscriptionAssignmentRegistry.createSnapshot(), view);

        // then
        wait.untilZookeeperPathIsCreated(basePath, s1.getQualifiedName().toString(), supervisorId);
    }

    @Test
    public void shouldApplyAssignmentChangesByRemovingInvalidNodes() {
        // given
        Subscription s1 = forceAssignment(anySubscription());

        // when
        workTracker.apply(subscriptionAssignmentRegistry.createSnapshot(), stateWithNoAssignments());

        // then
        wait.untilZookeeperPathNotExists(basePath, s1.getQualifiedName().toString(), supervisorId);
    }

    @Test
    public void shouldApplyAssignmentChangesByRemovingInvalidSubscriptionNode() {
        // given
        Subscription s1 = dropAssignment(forceAssignment(anySubscription()));

        // when
        workTracker.apply(subscriptionAssignmentRegistry.createSnapshot(), stateWithNoAssignments());

        // then
        wait.untilZookeeperPathNotExists(basePath, s1.getQualifiedName().toString());
    }

    @Test
    public void shouldApplyAssignmentChangesByRemovingSubscriptionNode() {
        // given
        Subscription s1 = anySubscription();
        SubscriptionAssignmentView view = stateWithSingleAssignment(s1);
        workTracker.apply(subscriptionAssignmentRegistry.createSnapshot(), view);
        wait.untilZookeeperPathIsCreated(basePath, s1.getQualifiedName().toString(), supervisorId);

        // when
        workTracker.apply(subscriptionAssignmentRegistry.createSnapshot(), stateWithNoAssignments());

        // then
        wait.untilZookeeperPathNotExists(basePath, s1.getQualifiedName().toString());
    }

    @Test
    public void shouldApplyAssignmentChangesByAddingNewNodes() {
        // given
        Subscription s1 = forceAssignment(anySubscription());
        Subscription s2 = forceAssignment(anySubscription());
        SubscriptionAssignmentView view = new SubscriptionAssignmentView(
                ImmutableMap.of(
                        s1.getQualifiedName(), ImmutableSet.of(assignment(supervisorId, s1.getQualifiedName()), assignment("otherConsumer", s1.getQualifiedName())),
                        s2.getQualifiedName(), ImmutableSet.of(assignment(supervisorId, s2.getQualifiedName()))));

        // when
        workTracker.apply(subscriptionAssignmentRegistry.createSnapshot(), view);

        // then
        wait.untilZookeeperPathIsCreated(basePath, s1.getQualifiedName().toString(), supervisorId);
        wait.untilZookeeperPathIsCreated(basePath, s1.getQualifiedName().toString(), "otherConsumer");
        wait.untilZookeeperPathIsCreated(basePath, s2.getQualifiedName().toString(), supervisorId);
    }

    private SubscriptionAssignmentView stateWithSingleAssignment(Subscription subscription) {
        return new SubscriptionAssignmentView(ImmutableMap.of(subscription.getQualifiedName(),
                ImmutableSet.of(assignment(supervisorId, subscription.getQualifiedName()))));
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
        wait.untilZookeeperPathIsCreated(basePath, sub.getQualifiedName().toString(), supervisorId);
        return sub;
    }

    private Subscription dropAssignment(Subscription sub) {
        workTracker.dropAssignment(sub);
        wait.untilZookeeperPathNotExists(basePath, sub.getQualifiedName().toString(), supervisorId);
        return sub;
    }
}
