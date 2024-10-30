package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.group.GroupNotEmptyException
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException
import pl.allegro.tech.hermes.infrastructure.MalformedDataException
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperGroupRepositoryTest extends IntegrationTest {

    private ZookeeperGroupRepository repository = new ZookeeperGroupRepository(zookeeper(), mapper, paths)
    
    def "should create group"() {
        when:
        repository.createGroup(group('createGroup').build())
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
        Group group = group('groupDetails').build()
        repository.createGroup(group)
        wait.untilGroupCreated('groupDetails')

        when:
        Group retrievedGroup = repository.getGroupDetails('groupDetails')

        then:
        retrievedGroup.groupName == 'groupDetails'
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
        repository.createGroup(group('nonemptyGroup').build())
        wait.untilGroupCreated('nonemptyGroup')
        topicRepository.createTopic(topic('nonemptyGroup', 'topic').build())
        wait.untilTopicCreated('nonemptyGroup', 'topic')
        
        when:
        repository.removeGroup('nonemptyGroup')
        
        then:
        thrown(GroupNotEmptyException)
    }

    def "should remove group when recently deleted a topic"() {
        given:
         Group group = group('removeGroup').build()
        repository.createGroup(group)
        wait.untilGroupCreated('removeGroup')
        topicRepository.createTopic(topic('removeGroup', 'remove').build())
        wait.untilTopicCreated('removeGroup', 'remove')
        topicRepository.removeTopic(new TopicName('removeGroup', 'remove'))
        wait.untilTopicRemoved('removeGroup', 'remove')

        when:
        repository.removeGroup('removeGroup')

        then:
        !repository.listGroupNames().contains('removeGroup')
    }

    def "should not throw exception on malformed topic when reading list of all topics"() {
        given:
        zookeeper().create().forPath(paths.groupPath('malformedGroup'), ''.bytes)
        wait.untilGroupCreated('malformedGroup')

        when:
        repository.listGroups()

        then:
        notThrown(MalformedDataException)
    }
}
