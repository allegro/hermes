package pl.allegro.tech.hermes.integrationtests.management;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.management.TestSecurityProvider;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class OfflineRetransmissionManagementTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  private static final String GROUP = "pl.allegro.retransmission";

  @BeforeAll
  public static void setupGroup() {
    hermes.initHelper().createGroup(Group.from(GROUP));
  }

  @AfterEach
  public void cleanup() {
    deleteTasks();
  }

  @Test
  public void shouldCreateTopicRetransmissionTask() {
    // given
    Topic sourceTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic targetTopic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // when
    OfflineRetransmissionRequest request =
        createSampleTopicRetransmissionRequest(
            sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);
    Instant now = Instant.now();

    // then
    response.expectStatus().isCreated();

    // and
    List<OfflineRetransmissionTask> allTasks = getOfflineRetransmissionTasks();
    assertThat(allTasks.size()).isEqualTo(1);
    assertThat(allTasks.get(0).getStartTimestamp()).isEqualTo(request.getStartTimestamp());
    assertThat(allTasks.get(0).getEndTimestamp()).isEqualTo(request.getEndTimestamp());
    assertThat(allTasks.get(0).getSourceTopic()).isEqualTo(request.getSourceTopic());
    assertThat(allTasks.get(0).getTargetTopic()).isEqualTo(request.getTargetTopic());
    assertThat(allTasks.get(0).getCreatedAt()).isBefore(now);
  }

  @Test
  public void shouldCreateViewRetransmissionTask() {
    // given
    var targetTopic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // when
    var request = createSampleViewRetransmissionRequest("testViewPath", targetTopic.getQualifiedName());
    var response = hermes.api().createOfflineRetransmissionTask(request);
    var now = Instant.now();

    // then
    response.expectStatus().isCreated();

    // and
    var allTasks = getOfflineRetransmissionTasks();
    assertThat(allTasks.size()).isEqualTo(1);
    assertThat(allTasks.get(0).getStartTimestamp()).isEmpty();
    assertThat(allTasks.get(0).getEndTimestamp()).isEmpty();
    assertThat(allTasks.get(0).getSourceTopic()).isEmpty();
    assertThat(allTasks.get(0).getSourceViewPath()).hasValue("testViewPath");
    assertThat(allTasks.get(0).getTargetTopic()).isEqualTo(request.getTargetTopic());
    assertThat(allTasks.get(0).getCreatedAt()).isBefore(now);
  }

  @Test
  public void shouldReturnEmptyListIfThereAreNoTasks() {
    // expect
    assertThat(getOfflineRetransmissionTasks().size()).isEqualTo(0);
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionWithEmptyData() {
    // given
    OfflineRetransmissionRequest request =
        new OfflineRetransmissionRequest(null, "", "", null, null);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            List.of(
                "must contain one defined source of retransmission data - source topic or source view"));
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionWithNotExistingSourceTopic() {
    // given
    Topic targetTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    OfflineRetransmissionRequest request =
        createSampleTopicRetransmissionRequest(
            "not.existing.sourceTopic", targetTopic.getQualifiedName());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains("Source topic does not exist");
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionWithNotExistingTargetTopic() {
    // given
    Topic sourceTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    OfflineRetransmissionRequest request =
        createSampleTopicRetransmissionRequest(
            sourceTopic.getQualifiedName(), "not.existing.targetTopic");

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains("Target topic does not exist");
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionWithNegativeTimeRange() {
    // given
    Topic sourceTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic targetTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    OfflineRetransmissionRequest request =
        new OfflineRetransmissionRequest(
            null,
            sourceTopic.getQualifiedName(),
            targetTopic.getQualifiedName(),
            Instant.now().toString(),
            Instant.now().minusSeconds(1).toString());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            "Must contain both startTimestamp and endTimestamp when source topic is given, startTimestamp must be lower than endTimestamp");
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionWithTargetTopicStoredOffline() {
    // given
    Topic sourceTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic targetTopic =
        hermes.initHelper().createTopic(topicWithRandomName().withOfflineStorage(1).build());
    OfflineRetransmissionRequest request =
        createSampleTopicRetransmissionRequest(
            sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains("Target topic must not be stored offline");
  }

  @Test
  public void shouldDeleteRetransmissionTask() {
    // given
    Topic sourceTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic targetTopic = hermes.initHelper().createTopic(topicWithRandomName().build());

    OfflineRetransmissionRequest request =
        createSampleTopicRetransmissionRequest(
            sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());
    hermes.api().createOfflineRetransmissionTask(request);

    List<OfflineRetransmissionTask> allTasks = getOfflineRetransmissionTasks();
    assertThat(allTasks.size()).isEqualTo(1);

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().deleteOfflineRetransmissionTask(allTasks.get(0).getTaskId());

    // then
    response.expectStatus().isOk();
    assertThat(getOfflineRetransmissionTasks().size()).isEqualTo(0);
  }

  @Test
  public void shouldReturnClientErrorWhenTryingToDeleteNotExistingRetransmissionTask() {
    // when
    String notExistingTaskId = "notExistingId";
    WebTestClient.ResponseSpec response =
        hermes.api().deleteOfflineRetransmissionTask(notExistingTaskId);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains("Retransmission task with id " + notExistingTaskId + " does not exist.");
  }

  @Test
  public void
      shouldThrowAccessDeniedWhenTryingToCreateTaskWithoutPermissionsToSourceAndTargetTopics() {
    // given
    Topic sourceTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Topic targetTopic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestSecurityProvider.setUserIsAdmin(false);
    OfflineRetransmissionRequest request =
        createSampleTopicRetransmissionRequest(
            sourceTopic.getQualifiedName(), targetTopic.getQualifiedName());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isForbidden();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains("User needs permissions to source and target topics");
    assertThat(getOfflineRetransmissionTasks().size()).isEqualTo(0);

    // cleanup
    TestSecurityProvider.reset();
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionFromViewWithTimestamps() {
    // given
    Topic targetTopic =
        hermes.initHelper().createTopic(topicWithRandomName().withOfflineStorage(1).build());

    OfflineRetransmissionRequest request =
        new OfflineRetransmissionRequest(
            "sourceView",
            null,
            targetTopic.getQualifiedName(),
            Instant.now().minusSeconds(5).toString(),
            Instant.now().toString());

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            List.of("must not contain startTimestamp and endTimestamp when source view is given"));
  }

  @Test
  public void shouldReturnClientErrorWhenRequestingRetransmissionFromTopicTableWithoutTimestamps() {
    // given
    Topic sourceTopic =
        hermes.initHelper().createTopic(topicWithRandomName().withOfflineStorage(1).build());
    Topic targetTopic =
        hermes.initHelper().createTopic(topicWithRandomName().withOfflineStorage(1).build());

    OfflineRetransmissionRequest request =
        new OfflineRetransmissionRequest(
            null, sourceTopic.getQualifiedName(), targetTopic.getQualifiedName(), null, null);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createOfflineRetransmissionTask(request);

    // then
    response.expectStatus().isBadRequest();
    assertThat(response.expectBody(String.class).returnResult().getResponseBody())
        .contains(
            List.of(
                "Must contain both startTimestamp and endTimestamp when source topic is given, startTimestamp must be lower than endTimestamp"));
  }

  private OfflineRetransmissionRequest createSampleTopicRetransmissionRequest(
      String sourceTopic, String targetTopic) {
    return new OfflineRetransmissionRequest(
        null,
        sourceTopic,
        targetTopic,
        Instant.now().minusSeconds(1).toString(),
        Instant.now().toString());
  }

  private OfflineRetransmissionRequest createSampleViewRetransmissionRequest(
      String sourceView, String targetTopic) {
    return new OfflineRetransmissionRequest(sourceView, null, targetTopic, null, null);
  }

  private void deleteTasks() {
    getOfflineRetransmissionTasks()
        .forEach(
            task ->
                hermes
                    .api()
                    .deleteOfflineRetransmissionTask(task.getTaskId())
                    .expectStatus()
                    .isOk());
  }

  @Nullable
  private static List<OfflineRetransmissionTask> getOfflineRetransmissionTasks() {
    return hermes
        .api()
        .getOfflineRetransmissionTasks()
        .expectStatus()
        .isOk()
        .expectBodyList(OfflineRetransmissionTask.class)
        .returnResult()
        .getResponseBody();
  }
}
