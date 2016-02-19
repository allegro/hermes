package pl.allegro.tech.hermes.frontend.cache.topic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class ZookeeperTopicsCacheTest extends ZookeeperBaseTest {

    public static final String QUALIFIED_NAME = "group.topic";
    public static final TopicName TOPIC_NAME = TopicName.fromQualifiedName(QUALIFIED_NAME);
    private ObjectMapper objectMapper = new ObjectMapper();
    private ConfigFactory configFactory = new ConfigFactory();
    private CountingTopicCallback callback = new CountingTopicCallback();
    private ZookeeperTopicsCache topicsCache;
    
    private ZookeeperPaths paths = new ZookeeperPaths("/hermes");
    private TopicRepository topicRepository;

    @Before
    public void setUp() throws Exception {
        zookeeperClient.create().creatingParentsIfNeeded().forPath("/hermes/groups");

        GroupRepository groupRepository = new ZookeeperGroupRepository(zookeeperClient, objectMapper, paths);
        topicRepository = new ZookeeperTopicRepository(zookeeperClient, objectMapper, paths, groupRepository);

        groupRepository.createGroup(Group.from("group"));

        topicsCache = new ZookeeperTopicsCache(zookeeperClient, configFactory, objectMapper);
        topicsCache.start(ImmutableList.of(callback));
    }

    @After
    public void after() throws Exception {
        deleteAllNodes();
        topicsCache.stop();
    }

    @Test
    public void shouldNotifyOfNewTopic() throws Exception {
        // when
        topicRepository.createTopic(topic(QUALIFIED_NAME).build());

        // then
        assertThat(callback.getCreateLatch().await(2000, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldNotifyOfTopicUpdate() throws Exception {
        // given
        topicRepository.createTopic(topic(QUALIFIED_NAME).build());
        waitUntilTopicIsCreated(TOPIC_NAME);

        // when
        topicRepository.updateTopic(topic(QUALIFIED_NAME).withRetentionTime(5).build());

        // then
        assertThat(callback.getChangeLatch().await(5000, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldNotifyOfRemovedTopics() throws Exception {
        // given
        topicRepository.createTopic(topic(QUALIFIED_NAME).build());
        waitUntilTopicIsCreated(TOPIC_NAME);

        // when
        topicRepository.removeTopic(TOPIC_NAME);

        // then
        assertThat(callback.getRemoveLatch().await(5000, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldTopicNotExistsAfterRemove() {
        // given
        topicRepository.createTopic(topic(QUALIFIED_NAME).build());
        waitUntilTopicIsCreated(TOPIC_NAME);

        // when
        topicRepository.removeTopic(TOPIC_NAME);

        // then
        await().until(() -> !topicsCache.getTopic(TOPIC_NAME).isPresent());
    }

    private void waitUntilTopicIsCreated(final TopicName topicName) {
        await().until(() -> {
            return topicsCache.getTopic(topicName).isPresent();
        });
    }

}