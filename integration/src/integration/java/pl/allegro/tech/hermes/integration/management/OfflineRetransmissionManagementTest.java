package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import java.time.Instant;
import java.util.List;
import javax.ws.rs.core.Response;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class OfflineRetransmissionManagementTest extends IntegrationTest {
    private static final String GROUP = "pl.allegro.retransmission";

    @BeforeClass
    public void setupClass() {
        operations.createGroup(GROUP);
    }

    @AfterMethod
    public void cleanup() {
        deleteTasks();
    }

    @Test
    public void shouldCreateRetransmissionTask() {
        // given
        Topic sourceTopic = createTopic();
        Topic targetTopic = createTopic();

        operations.buildTopic(sourceTopic);
        operations.buildTopic(targetTopic);

        // when
        OfflineRetransmissionRequest request = createRequest(
                sourceTopic.getQualifiedName(),
                targetTopic.getQualifiedName());
        Response response = management.offlineRetransmission().createRetransmissionTask(request);
        Instant now = Instant.now();

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);

        // and
        List<OfflineRetransmissionTask> allTasks = management.offlineRetransmission().getAllTasks();
        assertThat(allTasks.size()).isEqualTo(1);
        assertThat(allTasks.get(0).getStartTimestamp()).isEqualTo(request.getStartTimestamp());
        assertThat(allTasks.get(0).getEndTimestamp()).isEqualTo(request.getEndTimestamp());
        assertThat(allTasks.get(0).getSourceTopic()).isEqualTo(request.getSourceTopic());
        assertThat(allTasks.get(0).getTargetTopic()).isEqualTo(request.getTargetTopic());
        assertThat(allTasks.get(0).getCreatedAt()).isBefore(now);
    }


    @Test
    public void shouldReturnEmptyListIfThereAreNoTasks() {
        // expect
        assertThat(management.offlineRetransmission().getAllTasks().size()).isEqualTo(0);
    }

    @Test
    public void shouldReturnClientErrorWhenRequestingRetransmissionWithEmptyData() {
        // when
        OfflineRetransmissionRequest request = new OfflineRetransmissionRequest(
                "",
                "",
                null,
                null
        );
        Response response = management.offlineRetransmission().createRetransmissionTask(request);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        assertThat(response).containsMessages(
                "sourceTopic must not be empty",
                "targetTopic must not be empty",
                "startTimestamp must not be null",
                "endTimestamp must not be null");
    }

    @Test
    public void shouldReturnClientErrorWhenRequestingRetransmissionWithNotExistingSourceTopic() {
        // given
        Topic targetTopic = createTopic();

        // when
        OfflineRetransmissionRequest request = createRequest("not.existing.topic",
                targetTopic.getQualifiedName());
        Response response = management.offlineRetransmission().createRetransmissionTask(request);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        assertThat(response).containsMessage("Source topic does not exist");
    }

    @Test
    public void shouldReturnClientErrorWhenRequestingRetransmissionWithNotExistingTargetTopic() {
        // given
        Topic sourceTopic = createTopic();
        operations.buildTopic(sourceTopic);

        // when
        OfflineRetransmissionRequest request = createRequest(
                sourceTopic.getQualifiedName(), "not.existing.topic");
        Response response = management.offlineRetransmission().createRetransmissionTask(request);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        assertThat(response).containsMessage("Target topic does not exist");
    }

    @Test
    public void shouldReturnClientErrorWhenRequestingRetransmissionWithNegativeTimeRange() {
        // given
        Topic sourceTopic = createTopic();
        Topic targetTopic = createTopic();

        operations.buildTopic(sourceTopic);
        operations.buildTopic(targetTopic);

        // when
        OfflineRetransmissionRequest request = new OfflineRetransmissionRequest(
                sourceTopic.getQualifiedName(),
                targetTopic.getQualifiedName(),
                Instant.now(),
                Instant.now().minusSeconds(1));

        Response response = management.offlineRetransmission().createRetransmissionTask(request);
        assertThat(response).containsMessage("End timestamp must be greater than start timestamp");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldReturnClientErrorWhenRequestingRetransmissionWithTargetTopicStoredOffline() {
        // given
        Topic sourceTopic = createTopic();
        Topic targetTopic = TopicBuilder
                .randomTopic(GROUP, "test")
                .withOfflineStorage(1)
                .build();

        operations.buildTopic(sourceTopic);
        operations.buildTopic(targetTopic);

        // when
        OfflineRetransmissionRequest request = createRequest(
                sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());

        Response response = management.offlineRetransmission().createRetransmissionTask(request);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        assertThat(response).containsMessage("Target topic must not be stored offline");
    }


    @Test
    public void shouldDeleteRetransmissionTask() {
        // given
        Topic sourceTopic = createTopic();
        Topic targetTopic = createTopic();

        operations.buildTopic(sourceTopic);
        operations.buildTopic(targetTopic);

        OfflineRetransmissionRequest request = createRequest(
                sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());
        management.offlineRetransmission().createRetransmissionTask(request);

        List<OfflineRetransmissionTask> allTasks = management.offlineRetransmission().getAllTasks();
        assertThat(allTasks.size()).isEqualTo(1);

        // when
        Response response = management.offlineRetransmission().deleteRetransmissionTask(allTasks.get(0).getTaskId());

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        assertThat(management.offlineRetransmission().getAllTasks().size()).isEqualTo(0);
    }

    @Test
    public void shouldReturnClientErrorWhenTryingToDeleteNotExistingRetransmissionTask() {
        // when
        Response response = management.offlineRetransmission().deleteRetransmissionTask("notExistingId");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        assertThat(response).containsMessage("Retransmission task with id notExistingId does not exist.");
    }

    @Test
    public void shouldThrowAccessDeniedWhenTryingToCreateTaskWithoutPermissionsToSourceAndTargetTopics() {
        // given
        Topic sourceTopic = createTopic();
        Topic targetTopic = createTopic();

        operations.buildTopic(sourceTopic);
        operations.buildTopic(targetTopic);

        // when
        TestSecurityProvider.setUserIsAdmin(false);

        OfflineRetransmissionRequest request = createRequest(
                sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());
        Response response = management.offlineRetransmission().createRetransmissionTask(request);

        // then
        assertThat(response).hasStatus(Response.Status.FORBIDDEN);
        assertThat(response).containsMessage("User needs permissions to source and target topics");
        assertThat(management.offlineRetransmission().getAllTasks().size()).isEqualTo(0);

        // cleanup
        TestSecurityProvider.reset();
    }

    private OfflineRetransmissionRequest createRequest(String sourceTopic, String targetTopic) {
        return new OfflineRetransmissionRequest(
                sourceTopic,
                targetTopic,
                Instant.now().minusSeconds(1),
                Instant.now()
        );
    }

    private void deleteTasks() {
        management.offlineRetransmission().getAllTasks().forEach(task ->
                management.offlineRetransmission().deleteRetransmissionTask(task.getTaskId()));
    }

    private Topic createTopic() {
        return TopicBuilder
                .randomTopic(GROUP, "test")
                .build();
    }
}
