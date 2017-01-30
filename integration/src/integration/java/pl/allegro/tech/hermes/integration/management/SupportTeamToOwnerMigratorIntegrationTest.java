package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.management.migration.owner.SupportTeamToOwnerMigrator;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

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
        Topic firstSingleTeamTopic = operations.createTopic("migrationGroupSingleTeam", "firstSingleTeamTopic");
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
        SupportTeamToOwnerMigrator.ExecutionStats stats = response.readEntity(SupportTeamToOwnerMigrator.ExecutionStats.class);
        assertThat(stats.topics().migrated()).isEqualTo(4);
        assertThat(stats.subscriptions().migrated()).isEqualTo(3);
        assertThat(management.topic().get(firstSingleTeamTopic.getQualifiedName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", "Team A"));
        assertThat(management.subscription().get(firstSingleTeamTopicSubAlpha.getQualifiedTopicName(), firstSingleTeamTopicSubAlpha.getName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", "Team Alpha"));
        assertThat(management.subscription().get(firstSingleTeamTopicSubBeta.getQualifiedTopicName(), firstSingleTeamTopicSubBeta.getName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", "Team Beta"));

        assertThat(management.topic().get(secondSingleTeamTopic.getQualifiedName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", "Team A"));

        assertThat(management.topic().get(twoTeamsTopic.getQualifiedName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", "Team B, Team C"));
        assertThat(management.subscription().get(twoTeamsTopicSub.getQualifiedTopicName(), twoTeamsTopicSub.getName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", "Team Gamma, Team Delta"));

        assertThat(management.topic().get(noTeamTopic.getQualifiedName()).getOwner())
                .isEqualTo(new OwnerId("Plaintext", ""));
    }

    @Test
    public void shouldFailToMigrateIntoUnknownSource() {
        // when
        Response response = management.migration().execute("support-team-to-owner","unknown", true);

        // then
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
    }

}
