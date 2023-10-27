package pl.allegro.tech.hermes.integration.management;

import jakarta.ws.rs.core.Response;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.management.TestSecurityProvider;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.ErrorCode.GROUP_NAME_IS_INVALID;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.randomGroup;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class GroupManagementTest extends IntegrationTest {

    @Test
    public void shouldEmmitAuditEventWhenGroupCreated() {
        //given
        Group group = randomGroup("auditEventGroupCreated").build();

        //when
        operations.createGroup(group);

        //then
        assertThat(
                auditEvents.getLastRequest().getBodyAsString()
        ).contains("CREATED", group.getGroupName());
    }

    @Test
    public void shouldEmmitAuditEventWhenGroupRemoved() {
        //given
        Group group = randomGroup("auditEventGroupRemoved").build();
        operations.createGroup(group);

        //when
        operations.removeGroup(group);

        //then
        assertThat(
                auditEvents.getLastRequest().getBodyAsString()
        ).contains("REMOVED", group.getGroupName());
    }

    @Test
    public void shouldEmmitAuditEventWhenGroupUpdated() {
        //given
        Group group = randomGroup("auditEventGroupRemoved").build();
        operations.createGroup(group);

        //when
        operations.updateGroup(group);

        //then
        assertThat(
                auditEvents.getLastRequest().getBodyAsString()
        ).contains("UPDATED", group.getGroupName());
    }

    @Test
    public void shouldCreateGroup() {
        // given
        Group group = randomGroup("groupToCreate").build();

        // when
        operations.createGroup(group);

        // then
        assertThat(management.group().list()).contains(group.getGroupName());
    }

    @Test
    public void shouldListGroups() {
        // given
        Group group1 = randomGroup("listGroupsGroup1").build();
        Group group2 = randomGroup("listGroupsGroup2").build();
        operations.createGroup(group1);
        operations.createGroup(group2);

        // when then
        assertThat(management.group().list()).containsOnlyOnce(group1.getGroupName(), group2.getGroupName());
    }

    @Test
    void shouldCreateAndFetchGroupDetails() {
        //given
        Group group = randomGroup("groupWithDetails").build();
        operations.createGroup(group);

        //when
        Group fetchedGroup = management.group().get(group.getGroupName());

        //then
        assertThat(fetchedGroup).isEqualTo(group);
    }

    @Test
    public void shouldReturnBadRequestStatusWhenAttemptToCreateGroupWithInvalidCharactersWasMade() {
        // given
        Group groupWithNameWithSpaces = randomGroup("group;` name with spaces").build();

        // when
        Response response = management.group().create(groupWithNameWithSpaces);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST).hasErrorCode(GROUP_NAME_IS_INVALID);
    }

    @Test
    public void shouldRemoveGroup() {
        // given
        Group group = randomGroup("removeGroup").build();
        operations.createGroup(group);

        // when
        operations.removeGroup(group);

        // then
        assertThat(management.group().list()).doesNotContain(group.getGroupName());
    }

    @Test
    public void shouldAllowNonAdminUserToRemoveGroup() {
        // given
        Group group = randomGroup("removeGroupAsNonAdmin").build();
        TestSecurityProvider.setUserIsAdmin(false);
        operations.createGroup(group);

        // when
        operations.removeGroup(group);

        // then
        assertThat(management.group().list()).doesNotContain(group.getGroupName());

        // cleanup
        TestSecurityProvider.reset();
    }

    @Test
    public void shouldNotAllowOnRemovingNonEmptyGroup() {
        // given
        Group group = randomGroup("removeNonEmptyGroup").build();
        operations.createGroup(group);
        operations.createTopic(topic(group.getGroupName(), "topic").build());

        // when
        Response response = management.group().delete(group.getGroupName());

        // then
        assertThat(response).hasStatus(Response.Status.FORBIDDEN).hasErrorCode(ErrorCode.GROUP_NOT_EMPTY);
    }

    @Test
    public void shouldNotAllowDollarSigns() {
        Stream.of("$name", "na$me", "name$").forEach(name -> {
            // when
            Response response = management.group().create(randomGroup(name).build());

            // then
            assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        });
    }
}
