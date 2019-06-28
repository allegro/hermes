package pl.allegro.tech.hermes.management.domain.group

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.PatchData
import pl.allegro.tech.hermes.api.helpers.Patch
import pl.allegro.tech.hermes.domain.group.GroupRepository
import pl.allegro.tech.hermes.management.domain.Auditor
import pl.allegro.tech.hermes.management.domain.dc.MultiDcRepositoryCommandExecutor
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import spock.lang.Specification

import java.security.Principal

class GroupServiceSpec extends Specification {

    static String TEST_USER = "testUser"

    GroupRepository groupRepository = Stub()
    Auditor auditor = Mock()
    MultiDcRepositoryCommandExecutor executor = Stub()
    GroupService groupService = new GroupService(groupRepository, auditor, executor)

    def "should audit group creation"() {
        given:
            Group toBeCreated = GroupBuilder.group("testGroup").build()

        when:
            groupService.createGroup(toBeCreated, TEST_USER)

        then:
            1 * auditor.objectCreated(TEST_USER, toBeCreated)
    }

    def "should audit group removal"() {
        given:
            Group toBeRemoved = GroupBuilder.group("testGroup").build()

        when:
            groupService.removeGroup(toBeRemoved.groupName, TEST_USER)

        then:
            1 * auditor.objectRemoved(TEST_USER, Group.class.getSimpleName(), toBeRemoved.groupName)
    }

    def "should audit group update"() {
        given:
            Group toBeUpdated = GroupBuilder.group("testGroup").build()
            groupRepository.getGroupDetails(toBeUpdated.groupName) >> toBeUpdated
            PatchData groupPatch = PatchData.from(["groupName" : toBeUpdated.groupName,
                                                   "supportTeam": "modifiedSupportTeam"])
        when:
            groupService.updateGroup(toBeUpdated.groupName, groupPatch, TEST_USER)

        then:
            1 * auditor.objectUpdated(TEST_USER, toBeUpdated, Patch.apply(toBeUpdated, groupPatch))
    }

    Principal principal() {
        return new Principal() {
            @Override
            String getName() {
                return TEST_USER
            }
        };
    }
}
