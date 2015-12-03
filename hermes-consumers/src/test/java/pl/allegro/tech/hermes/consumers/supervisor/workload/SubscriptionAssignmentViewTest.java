package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SubscriptionAssignmentViewTest {

    @Test
    public void shouldNotDeleteFromEmptyView() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = emptySubscriptionAssignmentView();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").build();

        // when
        SubscriptionAssignmentView deletions = current.deletions(target);

        // then
        assertThat(deletions.getSubscriptions()).isEmpty();
    }

    @Test
    public void shouldAddToEmptyView() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = emptySubscriptionAssignmentView();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").build();

        // when
        SubscriptionAssignmentView additions = current.additions(target);

        // then
        assertThat(additions).isEqualTo(target);
    }

    @Test
    public void shouldNotAddToUnchangedView() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = assignmentView().withAssignment(s1, "c1").build();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").build();

        // when
        SubscriptionAssignmentView additions = current.additions(target);

        // then
        assertThat(additions.getSubscriptions()).isEmpty();
    }

    @Test
    public void shouldNotDeleteFromUnchangedView() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = assignmentView().withAssignment(s1, "c1").build();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").build();

        // when
        SubscriptionAssignmentView deletions = current.deletions(target);

        // then
        assertThat(deletions.getSubscriptions()).isEmpty();
    }

    @Test
    public void shouldAddAssignmentToExistingSubscription() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = assignmentView().withAssignment(s1, "c1").build();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").withAssignment(s1, "c2").build();

        // when
        SubscriptionAssignmentView additions = current.additions(target);

        // then
        assertThat(additions.getAssignmentsForSubscription(s1)).containsOnly(assignment(s1, "c2"));
    }

    @Test
    public void shouldDeleteAssignmentFromExistingSubscription() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = assignmentView().withAssignment(s1, "c1").withAssignment(s1, "c2").build();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").build();

        // when
        SubscriptionAssignmentView deletions = current.deletions(target);

        // then
        assertThat(deletions.getAssignmentsForSubscription(s1)).containsOnly(assignment(s1, "c2"));
    }

    @Test
    public void shouldAddNewSubscription() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionName s2 = anySubscriptionName();
        SubscriptionAssignmentView current = assignmentView().withAssignment(s1, "c1").build();
        SubscriptionAssignmentView target = assignmentView().withAssignment(s1, "c1").withAssignment(s2, "c1").build();

        // when
        SubscriptionAssignmentView additions = current.additions(target);

        // then
        assertThat(additions.getSubscriptions()).containsOnly(s2);
        assertThat(additions.getAssignmentsForSubscription(s2)).containsOnly(assignment(s2, "c1"));
    }

    @Test
    public void shouldDeleteOldSubscription() {
        // given
        SubscriptionName s1 = anySubscriptionName();
        SubscriptionAssignmentView current = assignmentView().withAssignment(s1, "c1").build();
        SubscriptionAssignmentView target = emptySubscriptionAssignmentView();

        // when
        SubscriptionAssignmentView deletions = current.deletions(target);

        // then
        assertThat(deletions.getSubscriptions()).isEqualTo(current.getSubscriptions());
        assertThat(deletions.getAssignmentsForSubscription(s1)).isEqualTo(current.getAssignmentsForSubscription(s1));
    }

    private SubscriptionAssignment assignment(SubscriptionName s1, String supervisorId) {
        return new SubscriptionAssignment(supervisorId, s1);
    }

    private SubscriptionName anySubscriptionName() {
        return SubscriptionName.fromString("com.example.topic$" + Math.abs(UUID.randomUUID().getMostSignificantBits()));
    }

    private static SubscriptionAssignmentView emptySubscriptionAssignmentView() {
        return assignmentView().build();
    }

    private static SubscriptionAssignmentViewBuilder assignmentView() {
        return new SubscriptionAssignmentViewBuilder();
    }

    private static class SubscriptionAssignmentViewBuilder {

        private SubscriptionAssignmentView assignmentView;

        private SubscriptionAssignmentViewBuilder() {
            assignmentView = new SubscriptionAssignmentView(Collections.emptyMap());
        }

        public SubscriptionAssignmentView build() {
            return SubscriptionAssignmentView.copyOf(assignmentView);
        }

        public SubscriptionAssignmentViewBuilder withAssignment(SubscriptionName subscriptionName, String consumerNodeId) {
            assignmentView = assignmentView.transform((view, transformer) -> {
                transformer.addSubscription(subscriptionName);
                transformer.addConsumerNode(consumerNodeId);
                transformer.addAssignment(new SubscriptionAssignment(consumerNodeId, subscriptionName));
            });
            return this;
        }
    }
}