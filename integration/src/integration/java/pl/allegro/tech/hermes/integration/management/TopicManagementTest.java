package pl.allegro.tech.hermes.integration.management;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.api.Topic.ContentType.*;
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
        Topic topic = operations.buildTopic("removeNonemptyTopicGroup", "topic");
        operations.createSubscription(topic,
                subscription().withName("subscription").withEndpoint(EndpointAddress.of("http://whatever.com")).withSupportTeam("team").applyDefaults().build());

        // when
        Response response = management.topic().remove("removeNonemptyTopicGroup.topic");

        // then
        assertThat(response).hasStatus(Response.Status.FORBIDDEN).hasErrorCode(ErrorCode.TOPIC_NOT_EMPTY);
    }

    @Test
    public void shouldRecreateTopicAfterDeletion() {
        // given
        operations.createGroup("recreateTopicGroup");
        Topic created = operations.createTopic("recreateTopicGroup", "topic");
        management.topic().remove("recreateTopicGroup.topic");

        wait.untilTopicRemovedInKafka(created);

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

    @Test
    public void shouldReturnTrackedTopicsWithAvroContentType() {
        // given
        operations.buildTopic(topic().withName("avroGroup", "avroTopic").withContentType(AVRO).withTrackingEnabled(false).build());
        operations.buildTopic(topic().withName("jsonGroup", "jsonTopic").withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopic(topic().withName("avroGroup", "avroTrackedTopic").withContentType(AVRO).withTrackingEnabled(true).build());
        operations.buildTopic(topic().withName("jsonGroup", "jsonTrackedTopic").withContentType(JSON).withTrackingEnabled(true).build());

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
        operations.buildTopic(topic().withName("mixedTrackedGroup", "avroTopic").withContentType(AVRO).withTrackingEnabled(false).build());
        operations.buildTopic(topic().withName("mixedTrackedGroup", "jsonTopic").withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopic(topic().withName("mixedTrackedGroup", "avroTrackedTopic").withContentType(AVRO).withTrackingEnabled(true).build());
        operations.buildTopic(topic().withName("mixedTrackedGroup", "jsonTrackedTopic").withContentType(JSON).withTrackingEnabled(true).build());

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
                    topic().withName("dollar", topic).applyDefaults().build());

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
                topic().withName(groupName, topicName).applyDefaults().build());

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
        management.topic().create(topic().withName(groupName, topicName).applyDefaults().build());

        // then
        assertThat(brokerOperations.topicExists(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME)).isTrue();
        assertThat(brokerOperations.topicExists(qualifiedTopicName, SECONDARY_KAFKA_CLUSTER_NAME)).isFalse();
    }

    @Test
    public void shouldUseDefaultContentTypeServerSide() {
        // given
        Topic topic = topic().withName("defaultContentType", "topic").build();

        // when
        operations.buildTopic(topic);

        // then
        assertThat(management.topic().get(topic.getQualifiedName()).getContentType()).isEqualTo(AVRO);
    }
}
