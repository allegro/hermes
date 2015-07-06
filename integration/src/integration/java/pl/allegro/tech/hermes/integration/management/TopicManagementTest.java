package pl.allegro.tech.hermes.integration.management;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import javax.ws.rs.core.Response;
import java.util.List;

import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class TopicManagementTest extends IntegrationTest {

    @Test
    public void shouldCreateTopic() {
        // given
        operations.createGroup("createTopicGroup");

        // when
        Response response = management.topic().create(
                topic().withName("createTopicGroup", "topic").applyDefaults().build());

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get("createTopicGroup.topic")).isNotNull();
    }

    @Test
    public void shouldListTopics() {
        // given
        operations.createGroup("listTopicsGroup");
        operations.createTopic("listTopicsGroup", "topic1");
        operations.createTopic("listTopicsGroup", "topic2");

        // when then
        Assertions.assertThat(management.topic().list("listTopicsGroup", false)).containsOnlyOnce(
                "listTopicsGroup.topic1", "listTopicsGroup.topic2");
    }

    @Test
    public void shouldRemoveTopic() {
        // given
        operations.createGroup("removeTopicGroup");
        operations.createTopic("removeTopicGroup", "topic");

        // when
        Response response = management.topic().remove("removeTopicGroup.topic");

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        Assertions.assertThat(management.topic().list("removeTopicGroup", false)).isEmpty();
    }

    @Test
    public void shouldNotAllowOnDeletingTopicWithSubscriptions() {
        // given
        operations.createGroup("removeNonemptyTopicGroup");
        operations.createTopic("removeNonemptyTopicGroup", "topic");
        operations.createSubscription("removeNonemptyTopicGroup", "topic",
                subscription().withName("subscription").withEndpoint(EndpointAddress.of("http://whatever.com")).applyDefaults().build());

        // when
        Response response = management.topic().remove("removeNonemptyTopicGroup.topic");

        // then
        assertThat(response).hasStatus(Response.Status.FORBIDDEN).hasErrorCode(ErrorCode.TOPIC_NOT_EMPTY);
    }

    @Test
    public void shouldNotAllowCreatingAvroTopicWithoutSchema() {
        // given
        operations.createGroup("createAvroGroup");

        // when
        Response response = management.topic().create(topic()
                .withName("createAvroGroup", "topic")
                .applyDefaults()
                .withContentType(Topic.ContentType.AVRO)
                .build());

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldNotAllowCreatingTopicWithInvalidAvroSchema() {
        // given
        operations.createGroup("createAvroInvalidSchemaGroup");

        // when
        Response response = management.topic().create(topic()
                .withName("createAvroInvalidSchemaGroup", "topic")
                .applyDefaults()
                .withContentType(Topic.ContentType.AVRO)
                .withMessageSchema("invalidSchema")
                .build());

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldRecreateTopicAfterDeletion() {
        // given
        operations.createGroup("recreateTopicGroup");
        operations.createTopic("recreateTopicGroup", "topic");
        management.topic().remove("recreateTopicGroup.topic");

        wait.untilKafkaZookeeperNodeDeletion("/brokers/topics/recreateTopicGroup.topic");

        // when
        Response response = management.topic().create(
                topic().withName("recreateTopicGroup", "topic").applyDefaults().build());

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get("recreateTopicGroup.topic")).isNotNull();
    }

    @Test
    public void shouldNotAllowOnCreatingSameTopicTwice() {
        // given
        operations.createGroup("overrideTopicGroup");
        operations.createTopic(topic().withName("overrideTopicGroup", "topic").build());

        // when
        Response response = management.topic().create(topic().withName("overrideTopicGroup", "topic").build());

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST).hasErrorCode(ErrorCode.TOPIC_ALREADY_EXISTS);
    }

    @Test
    public void shouldReturnTopicsThatAreCurrentlyTracked() {
        // given
        operations.buildTopic(topic().withName("trackedGroup", "topic").withTrackingEnabled(true).build());
        operations.buildTopic(topic().withName("untrackedGroup", "topic").withTrackingEnabled(false).build());

        // when
        List<String> tracked = management.topic().list("", true);

        // then
        assertThat(tracked).contains("trackedGroup.topic").doesNotContain("untrackedGroup.topic");
    }

    @Test
    public void shouldReturnTopicsThatAreCurrentlyTrackedForGivenGroup() {
        // given
        operations.buildTopic(topic().withName("mixedTrackedGroup", "trackedTopic").withTrackingEnabled(true).build());
        operations.buildTopic(topic().withName("mixedTrackedGroup", "untrackedTopic").withTrackingEnabled(false).build());

        // when
        List<String> tracked = management.topic().list("mixedTrackedGroup", true);

        // then
        assertThat(tracked).contains("mixedTrackedGroup.trackedTopic")
                           .doesNotContain("mixedTrackedGroup.untrackedTopic");
    }
}
