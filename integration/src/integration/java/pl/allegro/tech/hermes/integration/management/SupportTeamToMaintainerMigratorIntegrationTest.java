package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MaintainerDescriptor;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.management.migration.maintainer.SupportTeamToMaintainerMigrator;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class SupportTeamToMaintainerMigratorIntegrationTest extends IntegrationTest {

    public static final String ALL = "{\"query\": {}}";

    @BeforeMethod
    public void clearTopics() {
        management.query().queryTopics(ALL).forEach(topic ->
                management.topic().remove(topic.getQualifiedName()));
        wait.until(() -> management.query().queryTopics(ALL).isEmpty());
    }

    @Test
    public void shouldMigrateGroupsTopicsAndSubscriptionsToMaintainerModel() {
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
        Response response = management.migration().execute("support-team-to-maintainer", "Simple", true);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        SupportTeamToMaintainerMigrator.ExecutionStats stats = response.readEntity(SupportTeamToMaintainerMigrator.ExecutionStats.class);
        assertThat(stats.topics().migrated()).isEqualTo(4);
        assertThat(stats.subscriptions().migrated()).isEqualTo(3);
        assertThat(management.topic().get(firstSingleTeamTopic.getQualifiedName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", "Team A"));
        assertThat(management.subscription().get(firstSingleTeamTopicSubAlpha.getQualifiedTopicName(), firstSingleTeamTopicSubAlpha.getName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", "Team Alpha"));
        assertThat(management.subscription().get(firstSingleTeamTopicSubBeta.getQualifiedTopicName(), firstSingleTeamTopicSubBeta.getName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", "Team Beta"));

        assertThat(management.topic().get(secondSingleTeamTopic.getQualifiedName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", "Team A"));

        assertThat(management.topic().get(twoTeamsTopic.getQualifiedName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", "Team B, Team C"));
        assertThat(management.subscription().get(twoTeamsTopicSub.getQualifiedTopicName(), twoTeamsTopicSub.getName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", "Team Gamma, Team Delta"));

        assertThat(management.topic().get(noTeamTopic.getQualifiedName()).getMaintainer())
                .isEqualTo(new MaintainerDescriptor("Simple", ""));
    }

    @Test
    public void shouldFailToMigrateIntoUnknownSource() {
        // when
        Response response = management.migration().execute("support-team-to-maintainer","unknown", true);

        // then
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
    }

}
