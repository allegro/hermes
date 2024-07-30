package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException
import pl.allegro.tech.hermes.domain.topic.TopicAlreadyExistsException
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException
import pl.allegro.tech.hermes.infrastructure.MalformedDataException
import pl.allegro.tech.hermes.test.IntegrationTest

import java.time.Instant

import static pl.allegro.tech.hermes.metrics.PathContext.pathContext
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperTopicRepositoryTest extends IntegrationTest {

    private static final String GROUP = "topicRepositoryGroup"

    private ZookeeperTopicRepository repository = new ZookeeperTopicRepository(zookeeper(), mapper, paths, groupRepository)

    void setup() {
        if (!groupRepository.groupExists(GROUP)) {
            groupRepository.createGroup(Group.from(GROUP))
        }
    }

    def "should create topic"() {
        when:
        repository.createTopic(topic(GROUP, 'create').build())
        wait.untilTopicCreated(GROUP, 'create')

        then:
        repository.listTopicNames(GROUP).containsAll(['create'])
    }

    def "should throw exception when trying to create topic that already exists"() {
        given:
        repository.createTopic(topic(GROUP, 'duplicate').build())
        wait.untilTopicCreated(GROUP, 'duplicate')

        when:
        repository.createTopic(topic(GROUP, 'duplicate').build())

        then:
        thrown(TopicAlreadyExistsException)
    }

    def "should throw exception when trying to create topic for unknown group"() {
        when:
        repository.createTopic(topic('unknownGroup', 'unknown').build())

        then:
        thrown(GroupNotExistsException)
    }

    def "should list all topic names"() {
        given:
        repository.createTopic(topic(GROUP, 'listName1').build())
        repository.createTopic(topic(GROUP, 'listName2').build())
        wait.untilTopicCreated(GROUP, 'listName1')
        wait.untilTopicCreated(GROUP, 'listName2')

        when:
        List topics = repository.listTopicNames(GROUP)

        then:
        topics.containsAll(['listName1', 'listName2'])
    }

    def "should list all topics"() {
        given:
        Topic topic1 = topic(GROUP, 'list1').build()
        Topic topic2 = topic(GROUP, 'list2').build()
        repository.createTopic(topic1)
        repository.createTopic(topic2)

        wait.untilTopicCreated(GROUP, 'list1')
        wait.untilTopicCreated(GROUP, 'list2')


        when:
        List topics = repository.listTopics(GROUP)

        then:
        topics.containsAll([topic1, topic2])
    }

    def "should topics details by topic names"() {
        given:
        Topic topic1 = topic(GROUP, 'listByNames1').build()
        Topic topic2 = topic(GROUP, 'listByNames2').build()
        repository.createTopic(topic1)
        repository.createTopic(topic2)

        wait.untilTopicCreated(GROUP, 'listByNames1')
        wait.untilTopicCreated(GROUP, 'listByNames2')

        when:
        List topics = repository.getTopicsDetails([topic1.name, topic2.name])

        then:
        topics.containsAll([topic1, topic2])
    }

    def "should load topic details"() {
        given:
        def timestamp = Instant.now()
        repository.createTopic(topic(GROUP, 'details').withDescription('description').build())
        wait.untilTopicCreated(GROUP, 'details')

        when:
        Topic retrievedTopic = repository.getTopicDetails(new TopicName(GROUP, 'details'))

        then:
        retrievedTopic.description == 'description'

        and: 'createdAt and modifiedAt are greater than or equal to timestamp'
        retrievedTopic.createdAt.isAfter(timestamp)
        retrievedTopic.modifiedAt.isAfter(timestamp)
    }

    def "should update topic"() {
        given:
        repository.createTopic(topic(GROUP, 'update').withDescription('before update').build())
        wait.untilTopicCreated(GROUP, 'update')

        Topic modifiedTopic = topic(GROUP, 'update').withDescription('after update').build()

        when:
        repository.updateTopic(modifiedTopic)

        then:
        repository.getTopicDetails(new TopicName(GROUP, 'update')).description == 'after update'
    }

    def "should touch topic without changing it"() {
        given:
        def topic = topic(GROUP, 'touch').withDescription('before update').build()
        repository.createTopic(topic)
        wait.untilTopicCreated(GROUP, 'touch')

        when:
        repository.touchTopic(topic.getName())

        then:
        repository.getTopicDetails(topic.getName()).description == 'before update'
    }

    def "should throw exception when listing topics from unexistent group"() {
        when:
        repository.listTopics('unknownGroup')

        then:
        thrown(GroupNotExistsException)
    }

    def "should throw exception when fetching details of unexistent topic"() {
        when:
        repository.getTopicDetails(new TopicName(GROUP, 'unknown'))

        then:
        thrown(TopicNotExistsException)
    }

    def "should return empty list when no topics defined for group"() {
        when:
        groupRepository.createGroup(Group.from('emptyGroup'))

        then:
        repository.listTopics('emptyGroup') == []
    }

    def "should remove topic"() {
        given:
        repository.createTopic(topic(GROUP, 'remove').build())
        wait.untilTopicCreated(GROUP, 'remove')

        when:
        repository.removeTopic(new TopicName(GROUP, 'remove'))

        then:
        !repository.topicExists(new TopicName(GROUP, 'remove'))
    }

    def "should remove topic with metrics and without preview"() {
        given:
        def topicName = "topicWithMetrics"

        repository.createTopic(topic(GROUP, topicName).build())
        wait.untilTopicCreated(GROUP, topicName)

        def path = pathsCompiler.compile(BASE_ZOOKEEPER_PATH + ZookeeperCounterStorage.TOPIC_VOLUME_COUNTER, pathContext()
                .withGroup(GROUP)
                .withTopic(topicName)
                .build())
        zookeeper().create().creatingParentsIfNeeded().forPath(path, '1'.bytes)
        wait.untilZookeeperPathIsCreated(path)

        when:
        repository.removeTopic(new TopicName(GROUP, topicName))

        then:
        !repository.topicExists(new TopicName(GROUP, topicName))
    }

    def "should remove topic with metrics and preview"() {
        given: "a topic"
        Topic topic = topic(GROUP, "topicWithMetricsAndPreview").build()
        repository.createTopic(topic)
        wait.untilTopicCreated(GROUP, topic.getName().getName())

        and: "volume metric in zk for that topic"
        String metricPath = paths.topicMetricPath(topic.getName(), "volume")
        zookeeper().create().creatingParentsIfNeeded().forPath(metricPath, '1'.bytes)
        wait.untilZookeeperPathIsCreated(metricPath)

        and: "preview in zk for that topic"
        String previewPath = paths.topicPreviewPath(topic.getName())
        zookeeper().create().creatingParentsIfNeeded().forPath(previewPath , '1'.bytes)
        wait.untilZookeeperPathIsCreated(previewPath)

        when:
        repository.removeTopic(topic.getName())

        then:
        !repository.topicExists(topic.getName())
    }

    def "should not throw exception on malformed topic when reading list of all topics"() {
        given:
        zookeeper().create().forPath(paths.topicPath(new TopicName(GROUP, 'malformed')), ''.bytes)
        wait.untilTopicCreated(GROUP, 'malformed')

        when:
        repository.listTopics(GROUP)

        then:
        notThrown(MalformedDataException)
    }
}
