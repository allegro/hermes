package pl.allegro.tech.hermes.integrationtests.setup;

import com.jayway.awaitility.Duration;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.client.ManagementTestClient;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

public class HermesInitHelper {

    private final ManagementTestClient managementTestClient;

    public HermesInitHelper(String managementUrl) {
        managementTestClient = new ManagementTestClient(managementUrl);
    }

    public Topic createTopic(Topic topic) {
        createGroup(Group.from(topic.getName().getGroupName()));
        managementTestClient.createTopic(TopicWithSchema.topicWithSchema(topic, null))
                .expectStatus()
                .is2xxSuccessful();
        waitUntilTopicCreated(topic.getQualifiedName());
        return topic;
    }

    private void createGroup(Group group) {
        managementTestClient.createGroup(group)
            .expectStatus()
            .is2xxSuccessful();
        waitUntilGroupCreated(group.getGroupName());
    }

    private void waitUntilGroupCreated(String groupName) {
        waitAtMost(Duration.TEN_SECONDS)
            .until(() -> managementTestClient.getGroups().contains(groupName));
    }

    private void waitUntilTopicCreated(String topicQualifiedName) {
        waitAtMost(Duration.TEN_SECONDS)
            .until(() -> managementTestClient.getTopic(topicQualifiedName)
                .expectStatus()
                .is2xxSuccessful());
    }

    public Subscription createSubscription(Subscription subscription) {
        managementTestClient.createSubscription(subscription)
            .expectStatus()
            .is2xxSuccessful();
        waitUntilSubscriptionIsActive(subscription);
        return subscription;
    }

    public void waitUntilSubscriptionIsActive(Subscription subscription) {
        waitAtMost(Duration.TEN_SECONDS)
            .until(() -> {
                Subscription sub = managementTestClient.getSubscription(subscription.getQualifiedTopicName(), subscription.getName())
                    .expectStatus()
                    .is2xxSuccessful()
                    .expectBody(Subscription.class)
                    .returnResult()
                    .getResponseBody();
                assertThat(sub.getState()).isEqualTo(Subscription.State.ACTIVE);
            });
    }
}
