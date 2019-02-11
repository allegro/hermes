package pl.allegro.tech.hermes.integration.management;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class TopicManagementTest extends IntegrationTest {

    private static final String SCHEMA = AvroUserSchemaLoader.load().toString();

    @Test
    public void shouldCreateTopic() {
        // given
        operations.createGroup("createTopicGroup");

        // when
        Topic topic = topic("createTopicGroup", "topic").build();
        Response response = management.topic().create(topicWithSchema(topic));

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
                topicWithSchema(topic("recreateTopicGroup", "topic").build()));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get("recreateTopicGroup.topic")).isNotNull();
    }

    @Test
    public void shouldNotCreateInvalidTopic() {
        // given
        operations.createGroup("invalidTopicGroup");

        // when
        Response response = management.topic().create(
                topicWithSchema(topic("invalidTopicGroup", "topic").withMaxMessageSize(Topic.MAX_MESSAGE_SIZE + 1).build()));

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST).hasErrorCode(ErrorCode.VALIDATION_ERROR);
        Assertions.assertThat(management.topic().list("invalidTopicGroup", false)).isEmpty();
    }

    @Test
    public void shouldNotAllowOnCreatingSameTopicTwice() {
        // given
        operations.createGroup("overrideTopicGroup");
        operations.createTopic(topic("overrideTopicGroup", "topic").build());

        // when
        Response response = management.topic().create(
                topicWithSchema(topic("overrideTopicGroup", "topic").build()));

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST).hasErrorCode(ErrorCode.TOPIC_ALREADY_EXISTS);
    }

    @Test
    public void shouldAllowMigratingTopicFromJsonToAvroByExtendingTopicWithAvroSchema() throws Exception {
        // given
        operations.createGroup("jsonToAvroTopic");
        operations.createTopic(topic("jsonToAvroTopic", "topic").withContentType(JSON).build());

        // when
        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", AvroUserSchemaLoader.load().toString())
                .build();
        Response response = management.topic().update("jsonToAvroTopic.topic", patch);

        // when
        assertThat(response).hasStatus(OK);
    }

    @Test
    public void shouldNotAllowMigratingTopicFromJsonToAvroWhenProvidingInvalidSchema() throws Exception {
        // given
        operations.createGroup("jsonToAvroTopicWithInvalidSchema");
        operations.createTopic(topic("jsonToAvroTopicWithInvalidSchema", "topic").withContentType(JSON).build());

        // when
        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", "invalid...")
                .build();
        Response response = management.topic().update("jsonToAvroTopicWithInvalidSchema.topic", patch);

        // when
        assertThat(response).hasStatus(BAD_REQUEST);
        assertThat(response.readEntity(ErrorDescription.class).getCode()).isEqualTo(ErrorCode.SCHEMA_BAD_REQUEST);
    }

    @Test
    public void shouldAllowMigratingTopicFromJsonToAvroWithoutProvidingAvroSchemaWhenItsAlreadyAvailableInRegistry() throws Exception {
        // given
        operations.createGroup("jsonToAvroTopicAlreadyWithSchemaInRegistry");
        Topic topic = topic("jsonToAvroTopicAlreadyWithSchemaInRegistry", "topic").withContentType(JSON).build();
        operations.createTopic(topic);
        operations.saveSchema(topic, AvroUserSchemaLoader.load().toString());

        // when
        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .build();
        Response response = management.topic().update("jsonToAvroTopicAlreadyWithSchemaInRegistry.topic", patch);

        // when
        assertThat(response).hasStatus(OK);
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
        operations.buildTopicWithSchema(topicWithSchema(topic("avroGroup", "avroNonTrackedTopic").withContentType(AVRO)
                .withTrackingEnabled(false).build(), SCHEMA));
        operations.buildTopic(topic("jsonGroup", "jsonTopic")
                .withContentType(JSON).withTrackingEnabled(false).build());

        operations.buildTopicWithSchema(topicWithSchema(topic("avroGroup", "avroTrackedTopic")
                .withContentType(AVRO).withTrackingEnabled(true).build(), SCHEMA));

        operations.buildTopic(topic("jsonGroup", "jsonTrackedTopic").withContentType(JSON).withTrackingEnabled(true).build());

        // and
        String query = "{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}";

        // when
        List<String> tracked = management.topic().queryList("", query);

        // then
        assertThat(tracked).contains("avroGroup.avroTrackedTopic")
                .doesNotContain(
                        "avroGroup.avroNonTrackedTopic",
                        "jsonGroup.jsonTopic",
                        "jsonGroup.jsonTrackedTopic"
                );
    }

    @Test
    public void shouldReturnTrackedTopicsWithAvroContentTypeForGivenGroup() {
        // given
        operations.buildTopicWithSchema(topicWithSchema(topic("mixedTrackedGroup", "avroTopic").withContentType(AVRO)
                .withTrackingEnabled(false).build(), SCHEMA));
        operations.buildTopic(topic("mixedTrackedGroup", "jsonTopic").withContentType(JSON).withTrackingEnabled(false).build());
        operations.buildTopicWithSchema(topicWithSchema(topic("mixedTrackedGroup", "avroTrackedTopic").withContentType(AVRO)
                .withTrackingEnabled(true).build(), SCHEMA));
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
            Response response = management.topic().create(topicWithSchema(topic("dollar", topic).build()));

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
        Response response = management.topic().create(topicWithSchema(topic(groupName, topicName).build()));

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
        management.topic().create(topicWithSchema(topic(groupName, topicName).build()));

        // then
        assertThat(brokerOperations.topicExists(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME)).isTrue();
        assertThat(brokerOperations.topicExists(qualifiedTopicName, SECONDARY_KAFKA_CLUSTER_NAME)).isFalse();
    }

    @Test
    public void shouldCreateTopicWithMaxMessageSize() {
        // given
        Topic topic = TopicBuilder.topic("messageSize", "topic").withMaxMessageSize(2048).build();
        assertThat(management.group().create(new Group(topic.getName().getGroupName(), "a"))).hasStatus(CREATED);

        // when
        Response response = management.topic().create(topicWithSchema(topic));

        // then
        assertThat(response).hasStatus(CREATED);
        assertThat(management.topic().get(topic.getQualifiedName()).getMaxMessageSize()).isEqualTo(2048);
    }

    @Test
    public void shouldUpdateTopicWithMaxMessageSize() {
        // given
        Topic topic = TopicBuilder.topic("updateMessageSize", "topic").withMaxMessageSize(2048).build();
        operations.buildTopic(topic);
        PatchData maxMessageSize = PatchData.from(ImmutableMap.of("maxMessageSize", 1024));

        // when
        assertThat(management.topic().update(topic.getQualifiedName(), maxMessageSize)).hasStatus(OK);

        // then
        assertThat(management.topic().get(topic.getQualifiedName()).getMaxMessageSize()).isEqualTo(1024);
    }

    @Test
    public void shouldCreateTopicWithRestrictedSubscribing() {
        // given
        Topic topic = TopicBuilder.topic("restricted", "topic").withSubscribingRestricted().build();
        assertThat(management.group().create(new Group(topic.getName().getGroupName(), "topic"))).hasStatus(CREATED);

        // when
        Response response = management.topic().create(topicWithSchema(topic));

        // then
        assertThat(response).hasStatus(CREATED);
        assertThat(management.topic().get(topic.getQualifiedName()).isSubscribingRestricted()).isTrue();
    }

    @Test
    public void shouldUpdateTopicWithRestrictedSubscribing() {
        // given
        Topic topic = TopicBuilder.topic("updateRestricted", "topic").build();
        operations.buildTopic(topic);
        PatchData restricted = PatchData.from(ImmutableMap.of("subscribingRestricted", true));

        // when
        assertThat(management.topic().update(topic.getQualifiedName(), restricted)).hasStatus(OK);

        // then
        assertThat(management.topic().get(topic.getQualifiedName()).isSubscribingRestricted()).isTrue();
    }

    @Test
    public void shouldCreateTopicWithOfflineStorageSettings() {
        // given
        operations.createGroup("offlineStorage");

        // when
        Topic topic = topic("offlineStorage", "topic").withOfflineStorage(2).build();
        Response response = management.topic().create(topicWithSchema(topic));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get("offlineStorage.topic")
                .getOfflineStorage().getRetentionTime().getDuration()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateTopicWithOfflineStorageSettings() {
        // given
        Topic topic = TopicBuilder.topic("offlineStorageUpdate", "topic").build();
        operations.buildTopic(topic);
        PatchData offlineStorageEnabled = PatchData.from(ImmutableMap.of("offlineStorage", ImmutableMap.of("enabled", true)));

        // when
        assertThat(management.topic().update(topic.getQualifiedName(), offlineStorageEnabled)).hasStatus(OK);

        // then
        assertThat(management.topic().get(topic.getQualifiedName()).getTopic().getOfflineStorage().isEnabled()).isTrue();
    }
}
