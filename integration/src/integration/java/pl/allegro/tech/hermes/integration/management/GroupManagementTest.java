package pl.allegro.tech.hermes.integration.management;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import javax.ws.rs.core.Response;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class GroupManagementTest extends IntegrationTest {

    @Test
    public void shouldCreateGroup() {
        // given when
        Response response = management.group().create(group("testGroup").build());

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.group().list()).contains("testGroup");
    }

    @Test
    public void shouldListGroups() {
        // given
        operations.createGroup("listGroupsGroup1");
        operations.createGroup("listGroupsGroup2");

        // when then
        Assertions.assertThat(management.group().list()).containsOnlyOnce("listGroupsGroup1", "listGroupsGroup2");
    }

    @Test
    void shouldCreateAndFetchGroupDetails() throws InterruptedException {
        //given
        Group group = group("groupWithDetails").build();
        management.group().create(group);

        //when
        Group fetchedGroup = management.group().get(group.getGroupName());

        //then
        Assertions.assertThat(fetchedGroup).isEqualTo(group);
    }

    @Test
    public void shouldReturnBadRequestStatusWhenAttemptToCreateGroupWithInvalidCharactersWasMade() {
        // given
        Group groupWithNameWithSpaces = group("group;` name with spaces").build();

        // when
        Response response = management.group().create(groupWithNameWithSpaces);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST).hasErrorCode(VALIDATION_ERROR);
    }

    @Test
    public void shouldRemoveGroup() {
        // given
        operations.createGroup("removeGroup");

        // when
        Response response = management.group().delete("removeGroup");

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        assertThat(management.group().list()).doesNotContain("removeGroup");
    }

    @Test
    public void shouldNotAllowOnRemovingNonEmptyGroup() {
        // given
        operations.createGroup("removeNonemptyGroup");
        operations.createTopic(topic("removeNonemptyGroup", "topic").build());

        // when
        Response response = management.group().delete("removeNonemptyGroup");

        // then
        assertThat(response).hasStatus(Response.Status.FORBIDDEN).hasErrorCode(ErrorCode.GROUP_NOT_EMPTY);
    }

    @Test
    public void shouldNotAllowDollarSigns() {
        Stream.of("$name", "na$me", "name$").forEach(name -> {
            // when
            Response response = management.group().create(group(name).build());

            // then
            assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        });
    }
}
