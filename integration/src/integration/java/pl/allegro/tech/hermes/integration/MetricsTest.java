package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotReportMetricsToConfigStorageForRemovedSubscription() {
        // given
        Topic topic = operations.buildTopic("metricsAfterSubscriptionRemovedGroup", "topic");
        String subscriptionName1 = "subscription";
        operations.createSubscription(topic, subscriptionName1, remoteService.getUrl());
        remoteService.expectMessages(TestMessage.simple().body());

        assertThat(publisher.publish(topic.getQualifiedName(), TestMessage.simple().body())).isEqualTo(CREATED);
        remoteService.waitUntilReceived();

        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName1);

        // when
        management.subscription().remove(topic.getQualifiedName(), subscriptionName1);

        // then
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);

        String subscriptionName2 = "subscription2";
        operations.createSubscription(topic, subscriptionName2, remoteService.getUrl());
        management.topic().publishMessage(topic.getQualifiedName(), TestMessage.simple().body());
        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName2);
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);
    }
}
