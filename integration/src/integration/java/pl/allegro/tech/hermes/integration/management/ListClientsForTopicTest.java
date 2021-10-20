package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class ListClientsForTopicTest extends IntegrationTest {

    @Test
    public void shouldListClientsForTopic() {
        // given
        Topic topic = operations.buildTopic("subscribeGroup", "topic");
        String expectedResponse = "Smurfs, Admins";
        createSubscriptionForTopic("ownedSubscription1", "Smurfs");
        createSubscriptionForTopic("ownedSubscription2", "Admins");

        // then
        assertThat(listClientsForTopic(topic.getQualifiedName())).isEqualTo(expectedResponse);
    }

    @Test
    public void shouldListClientsForTopicWithoutRepeating() {
        // given
        Topic topic = operations.buildTopic("subscribeGroup", "topic");
        String expectedResponse = "Team A, Team B";
        createSubscriptionForTopic("ownedSubscription1", "Team A");
        createSubscriptionForTopic("ownedSubscription2", "Team A");
        createSubscriptionForTopic("ownedSubscription3", "Team B");
        createSubscriptionForTopic("ownedSubscription4", "Team B");

        // then
        assertThat(listClientsForTopic(topic.getQualifiedName())).isEqualTo(expectedResponse);
    }

    private void createSubscriptionForTopic(String subscriptionName, String ownerId) {
        Topic topic = operations.buildTopic("subscribeGroup", "topic");

        Subscription subscription = SubscriptionBuilder.subscription(topic, subscriptionName)
                .withOwner(new OwnerId("Plaintext", ownerId))
                .build();

        operations.createSubscription(topic, subscription);
    }

    private String listClientsForTopic(String topic) {
        return management.allTopicClientsEndpoint().getTopicClients(topic);
    }
}
