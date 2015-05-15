package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.domain.group.GroupNotEmptyException
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.api.Topic.Builder.topic

class ZookeeperGroupRepositoryTest extends IntegrationTest {

    private ZookeeperGroupRepository repository = new ZookeeperGroupRepository(zookeeper(), mapper, paths)
    
    def "should create group"() {
        when:
        repository.createGroup(Group.from('createGroup'))
        wait.untilGroupCreated('createGroup')

        then:
        repository.listGroupNames().contains('createGroup')
    }

    def "should list all groups"() {
        given:
        repository.createGroup(Group.from('listGroup1'))
        repository.createGroup(Group.from('listGroup2'))
        wait.untilGroupCreated('listGroup1')
        wait.untilGroupCreated('listGroup2')

        when:
        List groups = repository.listGroupNames()

        then:
        groups.containsAll(['listGroup1', 'listGroup2'])
    }

    def "should return group details"() {
        given:
        Group group = Group.Builder.group().withGroupName('groupDetails')
                .withSupportTeam('team').withTechnicalOwner('owner').build()
        repository.createGroup(group)
        wait.untilGroupCreated('groupDetails')

        when:
        Group retrievedGroup = repository.getGroupDetails('groupDetails')

        then:
        retrievedGroup.supportTeam == 'team'
        retrievedGroup.technicalOwner == 'owner'
    }

    def "should update group"() {
        given:
        repository.createGroup(Group.Builder.group().withGroupName('updateGroup').withSupportTeam('team1').build())
        wait.untilGroupCreated('updateGroup')

        Group modifiedGroup  = Group.Builder.group().withGroupName('updateGroup').withSupportTeam('skylab').build()

        when:
        repository.updateGroup(modifiedGroup)

        then:
        repository.getGroupDetails('updateGroup').supportTeam == 'skylab'
    }

    def "should throw group not exists exception when trying to update unknown group"() {
        when:
        repository.updateGroup(Group.from('doesNotExist'));

        then:
        thrown(GroupNotExistsException)
    }

    def "should remove group"() {
        given:
        repository.createGroup(Group.from('removeGroup'))
        wait.untilGroupCreated('removeGroup')

        when:
        repository.removeGroup('removeGroup')

        then:
        !repository.listGroupNames().contains('removeGroup')
    }

    def "should not allow on removing nonempty groups"() {
        given:
        repository.createGroup(Group.from('nonemptyGroup'))
        wait.untilGroupCreated('nonemptyGroup')
        topicRepository.createTopic(topic().withName('nonemptyGroup', 'topic').build())
        wait.untilTopicCreated('nonemptyGroup', 'topic')
        
        when:
        repository.removeGroup('nonemptyGroup')
        
        then:
        thrown(GroupNotEmptyException)
    }
}
