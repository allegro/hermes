package pl.allegro.tech.hermes.infrastructure.zookeeper

import org.apache.curator.framework.CuratorFramework
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperMessagePreviewRepositoryTest extends IntegrationTest {

    private CuratorFramework zookeeper = zookeeper()

    private ZookeeperMessagePreviewRepository repository = new ZookeeperMessagePreviewRepository(zookeeper, mapper, paths)

    def setup() {
        if (!groupRepository.groupExists('messagePreviewGroup')) {
            groupRepository.createGroup(Group.from('messagePreviewGroup'))
            topicRepository.createTopic(topic('messagePreviewGroup.topic-1').build())
            topicRepository.createTopic(topic('messagePreviewGroup.topic-2').build())
        }
    }

    def "should save and load messages"() {
        given:
            TopicsMessagesPreview preview = new TopicsMessagesPreview()
            preview.add(fromQualifiedName('messagePreviewGroup.topic-1'), new MessagePreview('topic-1-1'.bytes))
            preview.add(fromQualifiedName('messagePreviewGroup.topic-1'), new MessagePreview('topic-1-2'.bytes))
            preview.add(fromQualifiedName('messagePreviewGroup.topic-2'), new MessagePreview('topic-2-1'.bytes))

        when:
            repository.persist(preview)

        then:
            repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-1'))*.content == ['topic-1-1'.bytes, 'topic-1-2'.bytes]
            repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-2'))*.content == ['topic-2-1'.bytes]
    }

    def "should overwrite existing messages with new ones"() {
        given:
        TopicsMessagesPreview preview = new TopicsMessagesPreview()
        preview.add(fromQualifiedName('messagePreviewGroup.topic-2'), new MessagePreview('topic-2-1'.bytes))
        repository.persist(preview)

        TopicsMessagesPreview newPreview = new TopicsMessagesPreview()
        newPreview.add(fromQualifiedName('messagePreviewGroup.topic-2'), new MessagePreview('topic-2-2'.bytes))

        when:
        repository.persist(newPreview)

        then:
        repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-2'))*.content == ['topic-2-2'.bytes]
    }

    def "should return empty list if message preview cannot be deserialized"() {
        given:
            List storedPreviews = [ new MessagePreview('valid-message'.bytes), 'message-in-invalid-format'.bytes ]

            String previewPath = paths.topicPath(fromQualifiedName('messagePreviewGroup.topic-1'), ZookeeperPaths.PREVIEW_PATH)
            repository.ensurePathExists(previewPath)
            zookeeper.setData().forPath(previewPath, mapper.writeValueAsBytes(storedPreviews))
        when:
            List<MessagePreview> result = repository.loadPreview(fromQualifiedName('messagePreviewGroup.topic-1'))
        then:
            result == []
    }
}
