package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class ListSubscriptionForOwnerTest extends IntegrationTest {

    @Test
    public void shouldListSubscriptionsForOwnerId() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A");
        createSubscriptionForOwner("ownedSubscription2", "Team A");
        createSubscriptionForOwner("ownedSubscription3", "Team B");

        // then
        assertThat(listSubscriptionsForOwner("Team A")).containsExactly("ownedSubscription1", "ownedSubscription2");
    }

    @Test
    public void shouldListSubscriptionAfterNewSubscriptionIsAdded() {
        // given
        assertThat(listSubscriptionsForOwner("Team C").isEmpty());

        // when
        createSubscriptionForOwner("ownedSubscription4", "Team C");

        // then
        assertThat(listSubscriptionsForOwner("Team C")).containsExactly("ownedSubscription4");
    }

    @Test
    public void shouldListSubscriptionAfterOwnerIsChanged() {
        // given
        createSubscriptionForOwner("ownedSubscription5", "Team D");

        // then
        assertThat(listSubscriptionsForOwner("Team D")).containsExactly("ownedSubscription5");
        assertThat(listSubscriptionsForOwner("Team E")).isEmpty();

        // when
        operations.updateSubscription("subscribeGroup", "topic", "ownedSubscription5",
                PatchData.patchData().set("owner", new OwnerId("Plaintext", "Team E")).build());

        // then
        assertThat(listSubscriptionsForOwner("Team D")).isEmpty();
        assertThat(listSubscriptionsForOwner("Team E")).containsExactly("ownedSubscription5");
    }

    @Test
    public void shouldNotListTopicAfterIsDeleted() {
        // given
        createSubscriptionForOwner("ownedSubscription6", "Team F");
        createSubscriptionForOwner("ownedSubscription7", "Team G");

        // then
        assertThat(listSubscriptionsForOwner("Team F")).containsExactly("ownedSubscription6");
        assertThat(listSubscriptionsForOwner("Team G")).containsExactly("ownedSubscription7");

        // when
        management.subscription().remove("subscribeGroup.topic", "ownedSubscription6");

        // then
        assertThat(listSubscriptionsForOwner("Team F").isEmpty());
        assertThat(listSubscriptionsForOwner("Team G")).containsExactly("ownedSubscription7");
    }

    private void createSubscriptionForOwner(String subscriptionName, String ownerId) {
        Topic topic = operations.buildTopic("subscribeGroup", "topic");

        Subscription subscription = SubscriptionBuilder.subscription(topic, subscriptionName)
                .withOwner(new OwnerId("Plaintext", ownerId))
                .build();

        operations.createSubscription(topic, subscription);
    }

    private List<String> listSubscriptionsForOwner(String ownerId) {
        return management.subscriptionOwnershipEndpoint().listForOwner("Plaintext", ownerId)
                .stream()
                .map(Subscription::getName)
                .collect(Collectors.toList());
    }
}
