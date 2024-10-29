package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.ErrorCode.GROUP_NAME_IS_INVALID;
import static pl.allegro.tech.hermes.api.ErrorCode.GROUP_NOT_EMPTY;
import static pl.allegro.tech.hermes.integrationtests.management.TopicManagementTest.getErrorCode;
import static pl.allegro.tech.hermes.integrationtests.setup.HermesExtension.auditEvents;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.time.Duration;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.management.TestSecurityProvider;

public class GroupManagementTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldEmitAuditEventWhenGroupCreated() {
    // when
    Group group = hermes.initHelper().createGroup(groupWithRandomName().build());

    // then
    assertThat(auditEvents.getLastReceivedRequest().getBodyAsString())
        .contains("CREATED", group.getGroupName());
  }

  @Test
  public void shouldEmitAuditEventWhenGroupRemoved() {
    // given
    Group group = hermes.initHelper().createGroup(groupWithRandomName().build());

    // when
    hermes.api().deleteGroup(group.getGroupName());

    // then
    assertThat(auditEvents.getLastReceivedRequest().getBodyAsString())
        .contains("REMOVED", group.getGroupName());
  }

  @Test
  public void shouldEmitAuditEventWhenGroupUpdated() {
    // given
    Group group = hermes.initHelper().createGroup(groupWithRandomName().build());

    // when
    hermes.api().updateGroup(group.getGroupName(), group);

    // then
    assertThat(auditEvents.getLastReceivedRequest().getBodyAsString())
        .contains("UPDATED", group.getGroupName());
  }

  @Test
  public void shouldCreateGroup() {
    // given when
    Group group = groupWithRandomName().build();
    WebTestClient.ResponseSpec response = hermes.api().createGroup(group);

    // then
    response.expectStatus().isCreated();

    assertThat(hermes.api().getGroups()).contains(group.getGroupName());
  }

  @Test
  public void shouldListGroups() {
    // given
    Group group1 = hermes.initHelper().createGroup(groupWithRandomName().build());
    Group group2 = hermes.initHelper().createGroup(groupWithRandomName().build());

    // when then
    Assertions.assertThat(hermes.api().getGroups())
        .containsOnlyOnce(group1.getGroupName(), group2.getGroupName());
  }

  @Test
  public void shouldReturnBadRequestStatusWhenAttemptToCreateGroupWithInvalidCharactersWasMade() {
    // given
    Group groupWithNameWithSpaces = group("group;` name with spaces").build();

    // when
    WebTestClient.ResponseSpec response = hermes.api().createGroup(groupWithNameWithSpaces);

    // then
    response.expectStatus().isBadRequest();
    assertThat(getErrorCode(response)).isEqualTo(GROUP_NAME_IS_INVALID);
  }

  @Test
  public void shouldRemoveGroup() {
    // given
    Group group = hermes.initHelper().createGroup(groupWithRandomName().build());

    // when
    WebTestClient.ResponseSpec response = hermes.api().deleteGroup(group.getGroupName());

    // then
    response.expectStatus().isOk();
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                Assertions.assertThat(hermes.api().getGroups())
                    .doesNotContain(group.getGroupName()));
  }

  @Test
  public void shouldAllowNonAdminUserToRemoveGroup() {
    // given
    TestSecurityProvider.setUserIsAdmin(false);
    Group group = hermes.initHelper().createGroup(groupWithRandomName().build());

    // when
    WebTestClient.ResponseSpec response = hermes.api().deleteGroup(group.getGroupName());

    // then
    response.expectStatus().isOk();

    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                Assertions.assertThat(hermes.api().getGroups())
                    .doesNotContain(group.getGroupName()));

    // cleanup
    TestSecurityProvider.reset();
  }

  @Test
  public void shouldNotAllowOnRemovingNonEmptyGroup() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // when
    WebTestClient.ResponseSpec response = hermes.api().deleteGroup(topic.getName().getGroupName());

    // then
    response.expectStatus().isForbidden();
    assertThat(getErrorCode(response)).isEqualTo(GROUP_NOT_EMPTY);
  }

  @Test
  public void shouldNotAllowDollarSigns() {
    Stream.of("$name", "na$me", "name$")
        .forEach(
            name -> {
              // when
              WebTestClient.ResponseSpec response = hermes.api().createGroup(group(name).build());

              // then
              response.expectStatus().isBadRequest();
            });
  }
}
