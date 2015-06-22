package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

public class ZookeeperSubscriptionsCacheTest extends ZookeeperBaseTest {

    public static final String SUB_NAME = "sub";
    private ObjectMapper objectMapper = new ObjectMapper();
    private ConfigFactory configFactory = new ConfigFactory();
    private CountingSubscriptionCallback callback = new CountingSubscriptionCallback();
    private ZookeeperSubscriptionsCache subscriptionCache;
    
    private GroupRepository groupRepository;
    private TopicRepository topicRepository;
    private SubscriptionRepository subscriptionRepository;

    @Before
    public void setUp() throws Exception {
        zookeeperClient.create().creatingParentsIfNeeded().forPath("/hermes/groups");
        ZookeeperPaths paths = new ZookeeperPaths("/hermes");
        
        groupRepository = new ZookeeperGroupRepository(zookeeperClient, objectMapper, paths);
        topicRepository = new ZookeeperTopicRepository(zookeeperClient, objectMapper, paths, groupRepository);
        subscriptionRepository = new ZookeeperSubscriptionRepository(zookeeperClient, objectMapper, paths, topicRepository);
        
        subscriptionCache = new ZookeeperSubscriptionsCache(zookeeperClient, configFactory, objectMapper);
        subscriptionCache.start(ImmutableList.of(callback));
    }

    @After
    public void after() throws Exception {
        deleteAllNodes();
        subscriptionCache.stop();
    }

    @Test
    public void shouldNotifyOfNewSubscriptions() throws Exception {
        // given
        TopicName topicName = createTopic("new.topic");
        subscriptionRepository.createSubscription(subscription().withTopicName(topicName).withName(SUB_NAME).build());
        waitUntilSubscriptionIsCreated(topicName, SUB_NAME);

        // then
        assertThat(callback.getCreateLatch().await(2000, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldNotifyOfSubscriptionStateChanged() throws Exception {
        // given
        TopicName topicName = createTopic("update.topic");
        subscriptionRepository.createSubscription(subscription().withTopicName(topicName).withName(SUB_NAME).build());
        waitUntilSubscriptionIsCreated(topicName, SUB_NAME);

        // when
        subscriptionRepository.updateSubscriptionState(topicName, SUB_NAME, Subscription.State.SUSPENDED);

        // then
        assertThat(callback.getChangeLatch().await(5000, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldNotifyOfRemovedTopics() throws Exception {
        // given
        TopicName topicName = createTopic("remove.topic");
        subscriptionRepository.createSubscription(subscription().withTopicName(topicName).withName(SUB_NAME).build());
        waitUntilSubscriptionIsCreated(topicName, SUB_NAME);

        // when
        subscriptionRepository.removeSubscription(topicName, SUB_NAME);

        // then
        assertThat(callback.getRemoveLatch().await(5000, MILLISECONDS)).isTrue();
    }

    private void waitUntilSubscriptionIsCreated(final TopicName topicName, final String subscriptionName) {
        await().until(() -> {
            return subscriptionRepository.subscriptionExists(topicName, subscriptionName);
        });
    }

    private TopicName createTopic(String qualifiedTopicName) {
        TopicName topicName = fromQualifiedName(qualifiedTopicName);
        groupRepository.createGroup(Group.from(topicName.getGroupName()));
        topicRepository.createTopic(topic().applyDefaults().withName(topicName).build());

        return topicName;
    }

}