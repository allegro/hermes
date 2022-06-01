package pl.allegro.tech.hermes.management.domain.group

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.PatchData
import pl.allegro.tech.hermes.api.helpers.Patch
import pl.allegro.tech.hermes.domain.group.GroupRepository
import pl.allegro.tech.hermes.management.api.auth.CreatorRights
import pl.allegro.tech.hermes.management.config.GroupProperties
import pl.allegro.tech.hermes.management.domain.Auditor
import pl.allegro.tech.hermes.management.domain.GroupNameIsNotAllowedException
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException
import pl.allegro.tech.hermes.management.domain.auth.RequestUser
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import spock.lang.Specification

import java.security.Principal

class GroupServiceSpec extends Specification {

    static String TEST_USERNAME = "testUser"
    static RequestUser USER = new TestRequestUser(TEST_USERNAME, true)

    GroupRepository groupRepository = Stub()
    Auditor auditor = Mock()
    MultiDatacenterRepositoryCommandExecutor executor = Stub()
    GroupProperties groupProperties = new GroupProperties()
    GroupValidator validator = new GroupValidator(groupRepository, groupProperties)
    GroupService groupService = new GroupService(groupRepository, auditor, executor, validator)

    def "should audit group creation"() {
        given:
        Group toBeCreated = GroupBuilder.group("testGroup").build()

        when:
        groupService.createGroup(toBeCreated, USER, stubCreatorRights(true))

        then:
        1 * auditor.objectCreated(TEST_USERNAME, toBeCreated)
    }

    def "should not allow to create group if no creator rights for creation"() {
        given:
        Group toBeCreated = GroupBuilder.group("testGroup").build()

        when:
        groupService.createGroup(toBeCreated, USER, stubCreatorRights(false))

        then:
        thrown(PermissionDeniedException)
    }

    def "should not allow to create group if group name is invalid"() {
        given:
        Group toBeCreated = GroupBuilder.group("invalid:testGroup").build()

        when:
        groupService.createGroup(toBeCreated, USER, stubCreatorRights(true))
        
        then:
        thrown(GroupNameIsNotAllowedException)
    }

    def "should audit group removal"() {
        given:
        Group toBeRemoved = GroupBuilder.group("testGroup").build()

        when:
        groupService.removeGroup(toBeRemoved.groupName, USER)

        then:
        1 * auditor.objectRemoved(TEST_USERNAME, Group.from(toBeRemoved.groupName))
    }

    def "should audit group update"() {
        given:
        Group toBeUpdated = GroupBuilder.group("testGroup").build()
        groupRepository.getGroupDetails(toBeUpdated.groupName) >> toBeUpdated
        PatchData groupPatch = PatchData.from(["groupName"  : toBeUpdated.groupName,
                                               "supportTeam": "modifiedSupportTeam"])
        when:
        groupService.updateGroup(toBeUpdated.groupName, groupPatch, USER)

        then:
        1 * auditor.objectUpdated(TEST_USERNAME, toBeUpdated, Patch.apply(toBeUpdated, groupPatch))
    }

    Principal principal() {
        return new Principal() {

            @Override
            String getName() {
                return TEST_USERNAME
            }
        }
    }

    CreatorRights<Group> stubCreatorRights(boolean allow) {
        return new CreatorRights<Group>() {

            @Override
            boolean allowedToManage(Group entity) {
                return allow
            }

            @Override
            boolean allowedToCreate(Group entity) {
                return allow
            }
        }
    }


}
