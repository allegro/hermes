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
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import spock.lang.Specification

import java.security.Principal

class GroupServiceSpec extends Specification {

    static String TEST_USER = "testUser"

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
        groupService.createGroup(toBeCreated, TEST_USER, stubCreatorRights(true))

        then:
        1 * auditor.objectCreated(TEST_USER, toBeCreated)
    }

    def "should not allow to create group if no creator rights for creation"() {
        given:
        Group toBeCreated = GroupBuilder.group("testGroup").build()

        when:
        def exceptionThrown = false

        try {
            groupService.createGroup(toBeCreated, TEST_USER, stubCreatorRights(false))
        } catch (PermissionDeniedException ex) {
            exceptionThrown = true
        }

        then:
        exceptionThrown
    }

    def "should not allow to create group if group name is invalid"() {
        given:
        Group toBeCreated = GroupBuilder.group("invalid:testGroup").build()

        when:
        def exceptionThrown = false

        try {
            groupService.createGroup(toBeCreated, TEST_USER, stubCreatorRights(true))
        } catch (GroupNameIsNotAllowedException ex) {
            exceptionThrown = true
        }

        then:
        exceptionThrown
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
        PatchData groupPatch = PatchData.from(["groupName"  : toBeUpdated.groupName,
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
