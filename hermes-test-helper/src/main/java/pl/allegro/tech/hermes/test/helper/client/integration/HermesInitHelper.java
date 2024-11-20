package pl.allegro.tech.hermes.test.helper.client.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

import java.time.Duration;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.test.helper.environment.FrontendNotification;

public class HermesInitHelper {

  private final ManagementTestClient managementTestClient;
  private final FrontendNotification frontendNotification;

  public HermesInitHelper(int managementPort, FrontendNotification frontendNotification) {
    managementTestClient = new ManagementTestClient(managementPort);
    this.frontendNotification = frontendNotification;
  }

  public Topic createTopic(Topic topic) {
    createGroupIfMissing(Group.from(topic.getName().getGroupName()));
    managementTestClient
        .createTopic(TopicWithSchema.topicWithSchema(topic, null))
        .expectStatus()
        .is2xxSuccessful();
    frontendNotification.notifyTopicCreated(topic);
    waitUntilTopicCreated(topic.getQualifiedName());
    return topic;
  }

  public Topic createTopicWithSchema(TopicWithSchema topic) {
    createGroupIfMissing(Group.from(topic.getName().getGroupName()));
    managementTestClient.createTopic(topic).expectStatus().is2xxSuccessful();
    frontendNotification.notifyTopicCreated(topic.getTopic());
    waitUntilTopicCreated(topic.getQualifiedName());
    return topic;
  }

  public Topic notifyFrontendTopicBlacklisted(Topic topic) {
    frontendNotification.notifyTopicBlacklisted(topic);
    return topic;
  }

  public Topic notifyFrontendTopicUnblacklisted(Topic topic) {
    frontendNotification.notifyTopicunblacklisted(topic);
    return topic;
  }

  public Group createGroupIfMissing(Group group) {
    if (managementTestClient.getGroups().contains(group.getGroupName())) {
      return group;
    }

    managementTestClient.createGroup(group).expectStatus().is2xxSuccessful();
    waitUntilGroupCreated(group.getGroupName());
    return group;
  }

  public Group createGroup(Group group) {
    managementTestClient.createGroup(group).expectStatus().is2xxSuccessful();
    waitUntilGroupCreated(group.getGroupName());
    return group;
  }

  private void waitUntilGroupCreated(String groupName) {
    waitAtMost(Duration.ofMinutes(1))
        .until(() -> managementTestClient.getGroups().contains(groupName));
  }

  private void waitUntilTopicCreated(String topicQualifiedName) {
    waitAtMost(Duration.ofMinutes(1))
        .untilAsserted(
            () ->
                managementTestClient.getTopic(topicQualifiedName).expectStatus().is2xxSuccessful());
  }

  public Subscription createSubscription(Subscription subscription) {
    managementTestClient.createSubscription(subscription).expectStatus().is2xxSuccessful();
    waitUntilSubscriptionIsActive(subscription);
    return subscription;
  }

  public void waitUntilSubscriptionIsActive(Subscription subscription) {
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              Subscription sub =
                  managementTestClient
                      .getSubscription(subscription.getQualifiedTopicName(), subscription.getName())
                      .expectStatus()
                      .is2xxSuccessful()
                      .expectBody(Subscription.class)
                      .returnResult()
                      .getResponseBody();
              assertThat(sub.getState()).isEqualTo(Subscription.State.ACTIVE);
            });
  }

  public OAuthProvider createOAuthProvider(OAuthProvider provider) {
    managementTestClient.createOAuthProvider(provider).expectStatus().is2xxSuccessful();
    return provider;
  }
}
