package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class SupportTeamToOwnerMigratorIntegrationTest extends IntegrationTest {

    public static final String ALL = "{\"query\": {}}";

    @BeforeMethod
    public void clearTopics() {
        management.query().queryTopics(ALL).forEach(topic ->
                management.topic().remove(topic.getQualifiedName()));
        wait.until(() -> management.query().queryTopics(ALL).isEmpty());
    }

    @Test
    public void shouldMigrateGroupsTopicsAndSubscriptionsToOwnerModel() {
        // given
        operations.createGroup("migrationGroupSingleTeam", "Team A");
        Topic firstSingleTeamTopic = operations.buildTopic(randomTopic("migrationGroupSingleTeam", "firstSingleTeamTopic").build());
        Subscription firstSingleTeamTopicSubAlpha = operations.createSubscription(firstSingleTeamTopic, subscription(firstSingleTeamTopic, "firstSingleTeamTopicSubAlpha")
                .withSupportTeam("Team Alpha")
                .build()
        );
        Subscription firstSingleTeamTopicSubBeta = operations.createSubscription(firstSingleTeamTopic, subscription(firstSingleTeamTopic, "firstSingleTeamTopicSubBeta")
                .withSupportTeam("Team Beta")
                .build()
        );
        Topic secondSingleTeamTopic = operations.createTopic("migrationGroupSingleTeam", "secondSingleTeamTopic");

        operations.createGroup("migrationGroupTwoTeams", "Team B, Team C");
        Topic twoTeamsTopic = operations.createTopic("migrationGroupTwoTeams", "twoTeamsTopic");
        Subscription twoTeamsTopicSub = operations.createSubscription(twoTeamsTopic, subscription(twoTeamsTopic.getQualifiedName(), "twoTeamsTopicSub")
                .withSupportTeam("Team Gamma, Team Delta")
                .build()
        );

        operations.createGroup("migrationGroupNoTeam", null);
        Topic noTeamTopic = operations.createTopic("migrationGroupNoTeam", "noTeamTopic");

        // when
        Response response = management.migration().execute("support-team-to-owner", "Plaintext", true);

        // then
        assertThat(response.getStatus()).isEqualTo(200);

        assertTopicLoadedFromApiHasOwner(firstSingleTeamTopic, new OwnerId("Plaintext", "Team A"));
        assertSubscriptionLoadedFromApiHasOwner(firstSingleTeamTopicSubAlpha, new OwnerId("Plaintext", "Team Alpha"));
        assertSubscriptionLoadedFromApiHasOwner(firstSingleTeamTopicSubBeta, new OwnerId("Plaintext", "Team Beta"));

        assertTopicLoadedFromApiHasOwner(secondSingleTeamTopic, new OwnerId("Plaintext", "Team A"));

        assertTopicLoadedFromApiHasOwner(twoTeamsTopic, new OwnerId("Plaintext", "Team B, Team C"));
        assertSubscriptionLoadedFromApiHasOwner(twoTeamsTopicSub, new OwnerId("Plaintext", "Team Gamma, Team Delta"));

        assertTopicLoadedFromApiHasOwner(noTeamTopic, new OwnerId("Plaintext", ""));
    }

    @Test
    public void shouldFailToMigrateIntoUnknownSource() {
        // when
        Response response = management.migration().execute("support-team-to-owner", "unknown", true);

        // then
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
    }

    private void assertTopicLoadedFromApiHasOwner(Topic topic, OwnerId ownerId) {
        assertThat(management.topic().get(topic.getQualifiedName()).getOwner()).isEqualTo(ownerId);
    }

    private void assertSubscriptionLoadedFromApiHasOwner(Subscription subscription, OwnerId ownerId) {
        assertThat(management.subscription().get(subscription.getQualifiedTopicName(), subscription.getName()).getOwner()).isEqualTo(ownerId);
    }

}
