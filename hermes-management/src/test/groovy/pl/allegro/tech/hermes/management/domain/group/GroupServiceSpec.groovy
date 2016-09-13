package pl.allegro.tech.hermes.management.domain.group

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.PatchData
import pl.allegro.tech.hermes.api.helpers.Patch
import pl.allegro.tech.hermes.domain.group.GroupRepository
import pl.allegro.tech.hermes.management.domain.Auditor
import spock.lang.Specification

import java.security.Principal

class GroupServiceSpec extends Specification {

    static Group TEST_GROUP = new Group("testGroup", "testOwner", "testTeam", "testContact")
    static String TEST_USER = "testUser"

    GroupRepository groupRepository = Stub() {
        getGroupDetails(TEST_GROUP.groupName) >> TEST_GROUP
    }
    Auditor auditor = Mock()
    GroupService groupService = new GroupService(groupRepository, auditor)

    def "should audit group creation"() {
        when:
            groupService.createGroup(TEST_GROUP, principal())

        then:
            1 * auditor.objectCreated(TEST_USER, TEST_GROUP)
    }

    def "should audit group removal"() {
        when:
            groupService.removeGroup(TEST_GROUP.groupName, principal())

        then:
            1 * auditor.objectRemoved(TEST_USER, TEST_GROUP.groupName)
    }

    def "should audit group modification"() {
        given:
            PatchData groupPatch = PatchData.from(["groupName" : TEST_GROUP.groupName,
                                                   "technicalOwner": TEST_GROUP.technicalOwner,
                                                   "supportTeam": TEST_GROUP.supportTeam,
                                                   "contact": "modifiedContact"])
        when:
            groupService.updateGroup(TEST_GROUP.groupName, groupPatch, principal())

        then:
            1 * auditor.objectUpdated(TEST_USER, TEST_GROUP, Patch.apply(TEST_GROUP, groupPatch))
    }

    def Principal principal() {
        return new Principal() {
            @Override
            String getName() {
                return TEST_USER;
            }
        };
    }
}
