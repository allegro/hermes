package pl.allegro.tech.hermes.integration.management;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class TopicManagementTest extends IntegrationTest {

    @Test
    public void shouldCreateTopic() {
        // given
        operations.createGroup("createTopicGroup");

        // when
        Response response = management.topic().create(
                topic("createTopicGroup", "topic").build());

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
        Topic topic = operations.buildTopic("removeNonemptyTopicGroup", "topic");
        operations.createSubscription(topic, subscription(topic, "subscription").build());

        // when
        Response response = management.topic().remove("removeNonemptyTopicGroup.topic");

        // then
        assertThat(response).hasStatus(Response.Status.FORBIDDEN).hasErrorCode(ErrorCode.TOPIC_NOT_EMPTY);
    }

    @Test(enabled = false)
    @Unreliable
    public void shouldRecreateTopicAfterDeletion() {
        // given
        operations.createGroup("recreateTopicGroup");
        Topic created = operations.createTopic("recreateTopicGroup", "topic");
        management.topic().remove("recreateTopicGroup.topic");

        wait.untilTopicRemovedInKafka(created);

        // when
        Response response = management.topic().create(
                topic("recreateTopicGroup", "topic").build());

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get("recreateTopicGroup.topic")).isNotNull();
    }

    @Test
    public void shouldNotAllowOnCreatingSameTopicTwice() {
        // given
        operations.createGroup("overrideTopicGroup");
        operations.createTopic(topic("overrideTopicGroup", "topic").build());

        // when
        Response response = management.topic().create(topic("overrideTopicGroup", "topic").build());

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST).hasErrorCode(ErrorCode.TOPIC_ALREADY_EXISTS);
    }

    @Test
    public void shouldReturnTopicsThatAreCurrentlyTracked() {
        // given
        operations.buildTopic(topic("trackedGroup", "topic").withTrackingEnabled(true).build());
        operations.buildTopic(topic("untrackedGroup", "topic").withTrackingEnabled(false).build());

        // when
        List<String> tracked = management.topic().list("", true);

        // then
        assertThat(tracked).contains("trackedGroup.topic").doesNotContain("untrackedGroup.topic");
    }

    @Test
    public void shouldReturnTopicsThatAreCurrentlyTrackedForGivenGroup() {
        // given
        operations.buildTopic(topic("mixedTrackedGroup", "trackedTopic").withTrackingEnabled(true).build());
        operations.buildTopic(topic("mixedTrackedGroup", "untrackedTopic").withTrackingEnabled(false).build());

        // when
        List<String> tracked = management.topic().list("mixedTrackedGroup", true);

        // then
        assertThat(tracked).contains("mixedTrackedGroup.trackedTopic")
                           .doesNotContain("mixedTrackedGroup.untrackedTopic");
    }

    @Test
    public void shouldReturnTrackedTopicsWithAvroContentType() {
        // given
        operations.buildTopic(topic("avroGroup", "avroTopic").withContentType(AVRO).withTrackingEnabled(false).build());
        operations.buildTopic(topic("jsonGroup", "jsonTopic").withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopic(topic("avroGroup", "avroTrackedTopic").withContentType(AVRO).withTrackingEnabled(true).build());
        operations.buildTopic(topic("jsonGroup", "jsonTrackedTopic").withContentType(JSON).withTrackingEnabled(true).build());

        // and
        String query = "{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}";

        // when
        List<String> tracked = management.topic().queryList("", query);

        // then
        assertThat(tracked).contains("avroGroup.avroTrackedTopic")
                .doesNotContain(
                        "avroGroup.avroTopic",
                        "jsonGroup.jsonTopic",
                        "jsonGroup.jsonTrackedTopic"
                );
    }

    @Test
    public void shouldReturnTrackedTopicsWithAvroContentTypeForGivenGroup() {
        // given
        operations.buildTopic(topic("mixedTrackedGroup", "avroTopic").withContentType(AVRO).withTrackingEnabled(false).build());
        operations.buildTopic(topic("mixedTrackedGroup", "jsonTopic").withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopic(topic("mixedTrackedGroup", "avroTrackedTopic").withContentType(AVRO).withTrackingEnabled(true).build());
        operations.buildTopic(topic("mixedTrackedGroup", "jsonTrackedTopic").withContentType(JSON).withTrackingEnabled(true).build());

        // and
        String query = "{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}";

        // when
        List<String> tracked = management.topic().queryList("mixedTrackedGroup", query);

        // then
        assertThat(tracked).contains("mixedTrackedGroup.avroTrackedTopic")
                .doesNotContain(
                        "mixedTrackedGroup.avroTopic",
                        "mixedTrackedGroup.jsonTopic",
                        "mixedTrackedGroup.jsonTrackedTopic"
                );
    }

    @Test
    public void shouldNotAllowDollarSign() {
        // given
        operations.createGroup("dollar");

        Stream.of("$name", "na$me", "name$").forEach(topic -> {
            // when
            Response response = management.topic().create(
                    topic("dollar", topic).build());

            // then
            assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        });
    }

    @Test
    public void shouldCreateTopicEvenIfExistsInBrokers() {
        // given
        String groupName = "existingTopicFromExternalBroker";
        String topicName = "topic";
        String qualifiedTopicName = groupName + "." + topicName;

        brokerOperations.createTopic(qualifiedTopicName);
        operations.createGroup(groupName);

        // when
        Response response = management.topic().create(
                topic(groupName, topicName).build());

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get(qualifiedTopicName)).isNotNull();
    }

    @Test
    public void topicCreationRollbackShouldNotDeleteTopicOnBroker() throws Throwable {
        // given
        String groupName = "topicCreationRollbackShouldNotDeleteTopicOnBroker";
        String topicName = "topic";
        String qualifiedTopicName = groupName + "." + topicName;

        brokerOperations.createTopic(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME);
        operations.createGroup(groupName);

        // when
        management.topic().create(topic(groupName, topicName).build());

        // then
        assertThat(brokerOperations.topicExists(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME)).isTrue();
        assertThat(brokerOperations.topicExists(qualifiedTopicName, SECONDARY_KAFKA_CLUSTER_NAME)).isFalse();
    }

    @Test
    public void shouldCreateTopicWithMaxMessageSize() {
        // given
        Topic topic = TopicBuilder.topic("messageSize", "topic").withMaxMessageSize(2048).build();
        assertThat(management.group().create(new Group(topic.getName().getGroupName(), "a", "a", "a"))).hasStatus(CREATED);

        // when
        Response response = management.topic().create(topic);

        // then
        assertThat(response).hasStatus(CREATED);
        assertThat(management.topic().get(topic.getQualifiedName()).getMaxMessageSize()).isEqualTo(2048);
    }
}
