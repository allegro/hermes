package pl.allegro.tech.hermes.integrationtests.management;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosPolicy;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicLabel;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosMode.DATACENTER;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integrationtests.setup.HermesExtension.auditEvents;
import static pl.allegro.tech.hermes.integrationtests.setup.HermesExtension.brokerOperations;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class TopicManagementTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    private static final String SCHEMA = AvroUserSchemaLoader.load().toString();

    @AfterEach
    public void cleanup() {
        TestSecurityProvider.reset();
    }

    @Test
    public void shouldEmitAuditEventWhenTopicCreated() {
        //when
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        //then
        assertThat(
                auditEvents.getLastReceivedRequest().getBodyAsString()
        ).contains("CREATED", "Topic", topic.getQualifiedName());
    }

    @Test
    public void shouldEmitAuditEventWhenTopicRemoved() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        //when
        hermes.api().deleteTopic(topic.getQualifiedName()).expectStatus().isOk();

        //then
        assertThat(
                auditEvents.getLastReceivedRequest().getBodyAsString()
        ).contains("REMOVED", topic.getQualifiedName());
    }

    @Test
    public void shouldEmitAuditEventWhenTopicUpdated() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        PatchData patchData = PatchData.from(ImmutableMap.of("maxMessageSize", 2048));

        //when
        hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        assertThat(
                auditEvents.getLastReceivedRequest().getBodyAsString()
        ).contains("UPDATED", topic.getQualifiedName());
    }

    @Test
    public void shouldEmitAuditEventBeforeUpdateWhenWrongPatchDataKeyProvided() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        PatchData patchData = PatchData.from(ImmutableMap.of("someValue", 2048));

        //when
        hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        assertThat(
                auditEvents.getLastReceivedRequest().getBodyAsString()
        ).contains("BEFORE_UPDATE", topic.getQualifiedName(), "someValue", "2048");
    }

    @Test
    public void shouldCreateTopic() {
        //given
        TopicWithSchema topic = TopicWithSchema.topicWithSchema(topicWithRandomName().build());
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        // then
        response.expectStatus().isCreated();
        hermes.api().getTopicResponse(topic.getQualifiedName()).expectStatus().isOk().expectBody();
    }

    @Test
    public void shouldListTopics() {
        // given
        hermes.initHelper().createTopic(topic("listTopicsGroup.topic1").build());
        hermes.api().createTopic(new TopicWithSchema(topic("listTopicsGroup.topic2").build(), null));

        // when then
        assertThat(getGroupTopicsList("listTopicsGroup")).containsExactly("listTopicsGroup.topic1", "listTopicsGroup.topic2");
    }

    @Test
    public void shouldRemoveTopic() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        // when
        WebTestClient.ResponseSpec response = hermes.api().deleteTopic(topic.getQualifiedName());

        // then
        response.expectStatus().isOk();
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(getGroupTopicsList(topic.getName().getGroupName())).isEmpty());
    }

    @Test
    public void shouldUnblacklistTopicWhileDeleting() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.api().blacklistTopic(topic.getQualifiedName());

        // when
        WebTestClient.ResponseSpec response = hermes.api().deleteTopic(topic.getQualifiedName());

        // then
        response.expectStatus().isOk();
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(getGroupTopicsList(topic.getName().getGroupName())).isEmpty());
        assertThat(hermes.api().isTopicBlacklisted(topic.getQualifiedName()).isBlacklisted()).isFalse();
    }

    @Test
    public void shouldNotAllowOnDeletingTopicWithSubscriptions() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

        // when
        WebTestClient.ResponseSpec response = hermes.api().deleteTopic(topic.getQualifiedName());

        // then
        response.expectStatus().isForbidden();
        assertThat(getErrorCode(response)).isEqualTo(ErrorCode.TOPIC_NOT_EMPTY);
    }

    @Test
    public void shouldRemoveTopicWithRelatedSubscriptionsWhenAutoRemoveEnabled() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName())
                .withAutoDeleteWithTopicEnabled(true).build());

        // when
        WebTestClient.ResponseSpec response = hermes.api().deleteTopic(topic.getQualifiedName());

        // then
        response.expectStatus().isOk();

        // and
        hermes.api().getSubscriptionResponse(topic.getQualifiedName(), subscription.getName()).expectStatus().isBadRequest();
    }

    @Test
    public void shouldNotCreateInvalidTopic() {
        // given
        String groupName = "invalidTopicGroup";
        hermes.initHelper().createGroup(Group.from(groupName));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(
                topicWithSchema(topic(groupName, "shouldNotCreateInvalidTopic").withMaxMessageSize(Topic.MAX_MESSAGE_SIZE + 1).build()));

        // then
        response.expectStatus().isBadRequest();
        assertThat(getErrorCode(response)).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(getGroupTopicsList(groupName)).isEmpty();
    }

    @Test
    public void shouldNotCreateTopicWithMissingGroup() {
        // given no group

        // when
        TopicWithSchema topicWithSchema = topicWithSchema(topic("nonExistingGroup", "topic")
                .withContentType(AVRO)
                .withTrackingEnabled(false).build(), SCHEMA);
        WebTestClient.ResponseSpec createTopicResponse = hermes.api().createTopic(topicWithSchema);
        WebTestClient.ResponseSpec schemaResponse = hermes.api().getSchema(topicWithSchema.getQualifiedName());

        // then
        createTopicResponse.expectStatus().isNotFound();
        assertThat(getErrorCode(createTopicResponse)).isEqualTo(ErrorCode.GROUP_NOT_EXISTS);
        schemaResponse.expectStatus().isNoContent();
    }

    @Test
    public void shouldNotAllowOnCreatingSameTopicTwice() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(new TopicWithSchema(topic, null));

        // then
        response.expectStatus().isBadRequest();
        assertThat(getErrorCode(response)).isEqualTo(ErrorCode.TOPIC_ALREADY_EXISTS);
    }

    @Test
    public void shouldAllowMigratingTopicFromJsonToAvroByExtendingTopicWithAvroSchema() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withContentType(JSON).build());

        // when
        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", AvroUserSchemaLoader.load().toString())
                .build();
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patch);

        // when
        response.expectStatus().isOk();
    }

    @Test
    public void shouldNotAllowMigratingTopicFromJsonToAvroWhenProvidingInvalidSchema() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withContentType(JSON).build());

        // when
        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", "invalid...")
                .build();
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patch);

        // when
        response.expectStatus().isBadRequest();
        assertThat(getErrorCode(response)).isEqualTo(ErrorCode.SCHEMA_BAD_REQUEST);
    }

    @Test
    public void shouldAllowMigratingTopicFromJsonToAvroWithoutProvidingAvroSchemaWhenItsAlreadyAvailableInRegistry() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withContentType(JSON).build());
        hermes.api().saveSchema(topic.getQualifiedName(), false, AvroUserSchemaLoader.load().toString());

        // when
        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .build();
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patch);

        // when
        response.expectStatus().isOk();
    }

    @Test
    public void shouldReturnTopicsThatAreCurrentlyTracked() {
        // given
        Topic trackedTopic = hermes.initHelper().createTopic(topicWithRandomName().withTrackingEnabled(true).build());
        Topic untrackedTopic = hermes.initHelper().createTopic(topicWithRandomName().withTrackingEnabled(false).build());

        // when
        WebTestClient.ResponseSpec response = hermes.api().listTrackedTopics("");

        // then
        response.expectStatus().isOk();
        assertThat(
                Arrays.stream(Objects.requireNonNull(response.expectBody(String[].class).returnResult().getResponseBody())).toList()
        ).contains(trackedTopic.getQualifiedName()).doesNotContain(untrackedTopic.getQualifiedName());
    }

    @Test
    public void shouldReturnTopicsThatAreCurrentlyTrackedForGivenGroup() {
        // given
        String groupName = "mixedTrackedGroup1";
        Topic trackedTopic = hermes.initHelper().createTopic(topic(groupName, "trackedTopic1").withTrackingEnabled(true).build());
        hermes.initHelper().createTopic(topicWithRandomName().withTrackingEnabled(false).build());

        // when
        WebTestClient.ResponseSpec response = hermes.api().listTrackedTopics(groupName);

        // then
        response.expectStatus().isOk();
        assertThat(
                Arrays.stream(Objects.requireNonNull(response.expectBody(String[].class).returnResult().getResponseBody())).toList()
        ).containsExactly(trackedTopic.getQualifiedName());
    }

    @Test
    public void shouldReturnTrackedTopicsWithAvroContentType() {
        // given
        String group = "mixedTrackedGroup2";
        Topic trackedTopicAvro = hermes.initHelper().createTopicWithSchema(new TopicWithSchema(topic(group, "trackedAvroTopic2").withTrackingEnabled(true).withContentType(AVRO).build(), SCHEMA));
        Topic untrackedAvroTopic = hermes.initHelper().createTopicWithSchema(new TopicWithSchema(topicWithRandomName().withTrackingEnabled(false).withContentType(AVRO).build(), SCHEMA));
        Topic untrackedJsonTopic = hermes.initHelper().createTopic(topicWithRandomName().withTrackingEnabled(false).withContentType(JSON).build());
        Topic trackedJsonTopic = hermes.initHelper().createTopic(topicWithRandomName().withTrackingEnabled(true).withContentType(JSON).build());

        // and
        String query = "{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}";

        // when
        WebTestClient.ResponseSpec response = hermes.api().queryTopics("", query);

        // then
        assertThat(Arrays.stream(Objects.requireNonNull(response.expectBody(String[].class).returnResult().getResponseBody())).toList())
                .contains(trackedTopicAvro.getQualifiedName())
                .doesNotContain(untrackedJsonTopic.getQualifiedName(), untrackedAvroTopic.getQualifiedName(), trackedJsonTopic.getQualifiedName());
    }

    @Test
    public void shouldReturnTrackedTopicsWithAvroContentTypeForGivenGroup() {
        // given
        String group = "mixedTrackedGroup3";
        Topic trackedTopicAvro = hermes.initHelper().createTopicWithSchema(new TopicWithSchema(topic(group, "trackedAvroTopic3").withTrackingEnabled(true).withContentType(AVRO).build(), SCHEMA));
        hermes.api().createTopic(new TopicWithSchema(topic(group, "untrackedAvroTopic").withTrackingEnabled(false).withContentType(AVRO).build(), SCHEMA));
        hermes.api().createTopic(new TopicWithSchema(topic(group, "untrackedJsonTopic").withTrackingEnabled(false).withContentType(JSON).build(), null));
        hermes.api().createTopic(new TopicWithSchema(topic(group, "trackedJsonTopic").withTrackingEnabled(true).withContentType(JSON).build(), null));

        // and
        String query = "{\"query\": {\"trackingEnabled\": \"true\", \"contentType\": \"AVRO\"}}";

        // when
        WebTestClient.ResponseSpec response = hermes.api().queryTopics(group, query);

        // then
        assertThat(Arrays.stream(Objects.requireNonNull(response.expectBody(String[].class).returnResult().getResponseBody())).toList())
                .containsExactly(trackedTopicAvro.getQualifiedName())
                .doesNotContain(group + "." + "untrackedAvroTopic", group + "." + "untrackedJsonTopic", group + "." + "trackedJsonTopic");
    }

    @Test
    public void shouldNotAllowDollarSign() {
        // given
        String group = "dollar";
        hermes.initHelper().createGroup(Group.from(group));

        Stream.of("$name", "na$me", "name$").forEach(topicName -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().createTopic(new TopicWithSchema(topic(group, topicName).build(), null));

            // then
            response.expectStatus().isBadRequest();
        });
    }

    @Test
    public void shouldCreateTopicWithMaxMessageSize() {
        // given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName().withMaxMessageSize(2048).build(), null);
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        // then
        response.expectStatus().isCreated();
        int fetchedMessageSize = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getMaxMessageSize();
        assertThat(fetchedMessageSize).isEqualTo(2048);
    }

    @Test
    public void shouldUpdateTopicWithMaxMessageSize() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withMaxMessageSize(2048).build());
        PatchData patchData = PatchData.from(ImmutableMap.of("maxMessageSize", 1024));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        // then
        response.expectStatus().isOk();
        int fetchedMessageSize = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getMaxMessageSize();
        assertThat(fetchedMessageSize).isEqualTo(1024);
    }

    @Test
    public void shouldCreateTopicWithRestrictedSubscribing() {
        // given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName().withSubscribingRestricted().build(), null);
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        // then
        response.expectStatus().isCreated();
        boolean fetchedIsSubscribingRestricted = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().isSubscribingRestricted();
        assertThat(fetchedIsSubscribingRestricted).isTrue();
    }

    @Test
    public void shouldUpdateTopicWithRestrictedSubscribing() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        PatchData patchData = PatchData.from(ImmutableMap.of("subscribingRestricted", true));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        // then
        response.expectStatus().isOk();
        boolean fetchedIsSubscribingRestricted = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().isSubscribingRestricted();
        assertThat(fetchedIsSubscribingRestricted).isTrue();
    }

    @Test
    public void shouldCreateTopicWithOfflineStorageSettings() {
        // given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName().withOfflineStorage(2).build(), null);
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        // then
        response.expectStatus().isCreated();
        int fetchedOfflineStorageDurationTime = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getOfflineStorage().getRetentionTime().getDuration();
        assertThat(fetchedOfflineStorageDurationTime).isEqualTo(2);
    }

    @Test
    public void shouldUpdateTopicWithOfflineStorageSettings() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        PatchData patchData = PatchData.from(ImmutableMap.of("offlineStorage", ImmutableMap.of("enabled", true)));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        // then
        response.expectStatus().isOk();
        boolean fetchedIsOfflineStorageEnabled = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getOfflineStorage().isEnabled();
        assertThat(fetchedIsOfflineStorageEnabled).isTrue();
    }

    @Test
    public void shouldCreateTopicWithLabels() {
        //given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName()
                .withLabels(ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-2"),
                        new TopicLabel("label-3")
                ))
                .build(), null);
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        //when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isCreated();
        Set<TopicLabel> fetchedLabels = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getLabels();
        assertThat(fetchedLabels).containsAll(
                ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-2"),
                        new TopicLabel("label-3")
                )
        );
    }

    @Test
    public void shouldUpdateTopicWithLabels() {
        //given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName()
                .withLabels(ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-3")
                ))
                .build(), null);
        hermes.initHelper().createTopic(topic);

        //when
        PatchData patchData = PatchData.from(ImmutableMap.of(
                "labels", ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-2"),
                        new TopicLabel("label-3")
                ))
        );
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isOk();
        Set<TopicLabel> fetchedLabels = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getLabels();
        assertThat(fetchedLabels).containsAll(
                ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-2"),
                        new TopicLabel("label-3")
                )
        );
    }

    @Test
    public void shouldNotCreateTopicWithDisallowedLabels() {
        // given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName()
                .withLabels(ImmutableSet.of(
                        new TopicLabel("some-random-label"),
                        new TopicLabel("label-2"),
                        new TopicLabel("label-3")
                ))
                .build(), null);
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        //when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isBadRequest();
        assertThat(getErrorCode(response)).isEqualTo(ErrorCode.VALIDATION_ERROR);
        hermes.api().getTopicResponse(topic.getQualifiedName()).expectStatus().isNotFound();
    }

    @Test
    public void shouldNotUpdateTopicWithDisallowedLabels() {
        //given
        TopicWithSchema topic = new TopicWithSchema(topicWithRandomName()
                .withLabels(ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-3")
                ))
                .build(), null);
        hermes.initHelper().createTopic(topic);

        //when
        PatchData patchData = PatchData.from(ImmutableMap.of(
                "labels", ImmutableSet.of(
                        new TopicLabel("some-random-label"),
                        new TopicLabel("label-2"),
                        new TopicLabel("label-3")
                ))
        );
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isBadRequest();
        assertThat(getErrorCode(response)).isEqualTo(ErrorCode.VALIDATION_ERROR);
        Set<TopicLabel> fetchedLabels = hermes.api().getTopicResponse(topic.getQualifiedName()).expectBody(TopicWithSchema.class).returnResult().getResponseBody().getLabels();
        assertThat(fetchedLabels).containsAll(
                ImmutableSet.of(
                        new TopicLabel("label-1"),
                        new TopicLabel("label-3")
                )
        );
    }

    @Test
    public void shouldCreateTopicEvenIfExistsInBrokers() {
        // given
        String groupName = "existingTopicFromExternalBroker";
        String topicName = "topic";
        String qualifiedTopicName = groupName + "." + topicName;
        hermes.initHelper().createGroup(Group.from(groupName));

        brokerOperations.createTopic(qualifiedTopicName);

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic((topicWithSchema(topic(groupName, topicName).build())));

        // then
        response.expectStatus().isCreated();
        hermes.api().getTopicResponse(qualifiedTopicName).expectStatus().isOk();
    }

    @Test
    public void shouldNotAllowNonAdminUserCreateTopicWithFallbackToRemoteDatacenterEnabled() {
        // given
        TestSecurityProvider.setUserIsAdmin(false);
        TopicWithSchema topic = topicWithSchema(
                topicWithRandomName()
                        .withFallbackToRemoteDatacenterEnabled()
                        .build()
        );
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isBadRequest();
        assertThat(response.expectBody(String.class).returnResult().getResponseBody())
                .contains("User is not allowed to enable fallback to remote datacenter");
    }

    @Test
    public void shouldAllowAdminUserCreateTopicWithFallbackToRemoteDatacenterEnabled() {
        // given
        TestSecurityProvider.setUserIsAdmin(true);
        TopicWithSchema topic = topicWithSchema(
                topicWithRandomName()
                        .withFallbackToRemoteDatacenterEnabled()
                        .build()
        );
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isCreated();
    }

    @Test
    public void shouldNotAllowNonAdminUserToEnableFallbackToRemoteDatacenter() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSecurityProvider.setUserIsAdmin(false);
        PatchData patchData = PatchData.from(ImmutableMap.of("fallbackToRemoteDatacenterEnabled", true));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isBadRequest();
        assertThat(response.expectBody(String.class).returnResult().getResponseBody())
                .contains("User is not allowed to enable fallback to remote datacenter");
    }

    @Test
    public void shouldAllowAdminUserToEnableFallbackToRemoteDatacenter() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSecurityProvider.setUserIsAdmin(true);
        PatchData patchData = PatchData.from(ImmutableMap.of("fallbackToRemoteDatacenterEnabled", true));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isOk();
    }

    @Test
    public void shouldAllowNonAdminUserToModifyTopicWithFallbackToRemoteDatacenterEnabled() {
        // given
        TestSecurityProvider.setUserIsAdmin(true);
        Topic topic = hermes.initHelper().createTopic(
                topicWithRandomName()
                        .withFallbackToRemoteDatacenterEnabled()
                        .build()
        );
        TestSecurityProvider.setUserIsAdmin(false);
        PatchData patchData = PatchData.from(ImmutableMap.of("description", "new description"));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isOk();
    }

    @Test
    public void shouldNotAllowNonAdminUserCreateTopicWithChaosEnabled() {
        // given
        TestSecurityProvider.setUserIsAdmin(false);
        TopicWithSchema topic = topicWithSchema(
                topicWithRandomName()
                        .withPublishingChaosPolicy(new PublishingChaosPolicy(DATACENTER, null, Map.of()))
                        .build()
        );
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isBadRequest();
        assertThat(response.expectBody(String.class).returnResult().getResponseBody())
                .contains("User is not allowed to set chaos policy for this topic");
    }

    @Test
    public void shouldAllowAdminUserCreateTopicWithChaosEnabled() {
        // given
        TestSecurityProvider.setUserIsAdmin(true);
        TopicWithSchema topic = topicWithSchema(
                topicWithRandomName()
                        .withPublishingChaosPolicy(new PublishingChaosPolicy(DATACENTER, null, Map.of()))
                        .build()
        );
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isCreated();
    }

    @Test
    public void shouldNotCreateTopicWithInvalidChaosPolicy() {
        // given
        TestSecurityProvider.setUserIsAdmin(true);
        TopicWithSchema topic = topicWithSchema(
                topicWithRandomName()
                        .withPublishingChaosPolicy(
                                new PublishingChaosPolicy(DATACENTER, null, Map.of("dc1", new ChaosPolicy(100, 100, 99, false)))
                        )
                        .build()
        );
        hermes.initHelper().createGroup(Group.from(topic.getName().getGroupName()));

        // when
        WebTestClient.ResponseSpec response = hermes.api().createTopic(topic);

        //then
        response.expectStatus().isBadRequest();
        assertThat(response.expectBody(String.class).returnResult().getResponseBody())
                .contains("Invalid chaos policy: 'delayFrom' and 'delayTo' must be >= 0, and 'delayFrom' <= 'delayTo'.");
    }

    @Test
    public void shouldNotAllowNonAdminUserToEnableChaos() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSecurityProvider.setUserIsAdmin(false);
        PatchData patchData = PatchData.from(ImmutableMap.of("chaos", ImmutableMap.of("mode", DATACENTER)));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isBadRequest();
        assertThat(response.expectBody(String.class).returnResult().getResponseBody())
                .contains("User is not allowed to update chaos policy for this topic");
    }

    @Test
    public void shouldAllowAdminUserToEnableChaos() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSecurityProvider.setUserIsAdmin(true);
        PatchData patchData = PatchData.from(ImmutableMap.of("chaos", ImmutableMap.of("mode", DATACENTER)));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isOk();
    }

    @Test
    public void shouldAllowNonAdminUserToModifyTopicWithChaosEnabled() {
        // given
        TestSecurityProvider.setUserIsAdmin(true);
        Topic topic = hermes.initHelper().createTopic(
                topicWithRandomName()
                        .withPublishingChaosPolicy(new PublishingChaosPolicy(DATACENTER, null, Map.of()))
                        .build()
        );
        TestSecurityProvider.setUserIsAdmin(false);
        PatchData patchData = PatchData.from(ImmutableMap.of("description", "new description"));

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isOk();
    }

    @Test
    public void shouldNotUpdateTopicWithInvalidChaosPolicy() {
        // given
        TestSecurityProvider.setUserIsAdmin(true);
        Topic topic = hermes.initHelper().createTopic(
                topicWithRandomName()
                        .withPublishingChaosPolicy(
                                new PublishingChaosPolicy(DATACENTER, null, Map.of("dc1", new ChaosPolicy(100, 100, 100, false)))
                        )
                        .build()
        );
        PatchData patchData = PatchData.from(
                ImmutableMap.of(
                        "chaos",
                        ImmutableMap.of(
                                "datacenterPolicies",
                                ImmutableMap.of("dc1", ImmutableMap.of("delayTo", 99))
                        )
                )
        );

        // when
        WebTestClient.ResponseSpec response = hermes.api().updateTopic(topic.getQualifiedName(), patchData);

        //then
        response.expectStatus().isBadRequest();
        assertThat(response.expectBody(String.class).returnResult().getResponseBody())
                .contains("Invalid chaos policy: 'delayFrom' and 'delayTo' must be >= 0, and 'delayFrom' <= 'delayTo'.");
    }

    private static List<String> getGroupTopicsList(String groupName) {
        return Arrays.stream(Objects.requireNonNull(hermes.api().listTopics(groupName)
                        .expectStatus()
                        .isOk()
                        .expectBody(String[].class)
                        .returnResult()
                        .getResponseBody()))
                .toList();
    }

    public static ErrorCode getErrorCode(WebTestClient.ResponseSpec createTopicResponse) {
        return Objects.requireNonNull(createTopicResponse.expectBody(ErrorDescription.class).returnResult().getResponseBody()).getCode();
    }
}
