package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperMessagePreviewRepositoryTest extends IntegrationTest {

    private ZookeeperMessagePreviewRepository repository = new ZookeeperMessagePreviewRepository(zookeeper(), mapper, paths)

    def setup() {
        if(!groupRepository.groupExists('messagePreviewGroup')) {
            groupRepository.createGroup(Group.from('messagePreviewGroup'))
            topicRepository.createTopic(topic('messagePreviewGroup.topic-1').build())
            topicRepository.createTopic(topic('messagePreviewGroup.topic-2').build())
        }
    }

    def "should save and load messages"() {
        given:
        TopicsMessagesPreview preview = new TopicsMessagesPreview()
        preview.add(fromQualifiedName('messagePreviewGroup.topic-1'), 'topic-1-1'.bytes)
        preview.add(fromQualifiedName('messagePreviewGroup.topic-1'), 'topic-1-2'.bytes)
        preview.add(fromQualifiedName('messagePreviewGroup.topic-2'), 'topic-2-1'.bytes)

        when:
        repository.persist(preview)

        then:
        repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-1')) == ['topic-1-1'.bytes, 'topic-1-2'.bytes]
        repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-2')) == ['topic-2-1'.bytes]
    }

    def "should overwrite existing messages with new ones"() {
        given:
        TopicsMessagesPreview preview = new TopicsMessagesPreview()
        preview.add(fromQualifiedName('messagePreviewGroup.topic-2'), 'topic-2-1'.bytes)
        repository.persist(preview)

        TopicsMessagesPreview newPreview = new TopicsMessagesPreview()
        newPreview.add(fromQualifiedName('messagePreviewGroup.topic-2'), 'topic-2-2'.bytes)

        when:
        repository.persist(newPreview)

        then:
        repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-2')) == ['topic-2-2'.bytes]
    }
}
