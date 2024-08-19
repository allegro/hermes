package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZookeeperCounterStorageTest {

    @Mock
    private SharedCounter sharedCounter;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private ZookeeperCounterStorage storage;

    @Before
    public void initialize() {
        PathsCompiler pathCompiler = new PathsCompiler("my-host-example.net");
        storage = new ZookeeperCounterStorage(sharedCounter, subscriptionRepository, pathCompiler, "/hermes");
    }

    @Test
    public void shouldIncrementTopicMetricUsingSharedCounter() {
        //when
        storage.setTopicPublishedCounter(TopicName.fromQualifiedName("test.topic"), 10);

        // then
        verify(sharedCounter).increment("/hermes/groups/test/topics/topic/metrics/published", 10);
    }

    @Test
    public void shouldReadValueFromTopicMetric() {
        // given
        when(sharedCounter.getValue("/hermes/groups/test/topics/topic/metrics/published")).thenReturn(10L);

        // when
        long value = storage.getTopicPublishedCounter(TopicName.fromQualifiedName("test.topic"));

        // then
        assertThat(value).isEqualTo(10);
    }

    @Test
    public void shouldIncrementSubscriptionMetricUsingSharedCounter() {
        // given when
        storage.setSubscriptionDeliveredCounter(TopicName.fromQualifiedName("test.topic"), "sub", 10);

        // then
        verify(sharedCounter).increment("/hermes/groups/test/topics/topic/subscriptions/sub/metrics/delivered", 10);
    }

    @Test
    public void shouldReadValueFromSubscriptionMetric() {
        // given
        when(sharedCounter.getValue("/hermes/groups/test/topics/topic/subscriptions/sub/metrics/delivered")).thenReturn(10L);

        // when
        long value = storage.getSubscriptionDeliveredCounter(TopicName.fromQualifiedName("test.topic"), "sub");

        // then
        assertThat(value).isEqualTo(10);
    }

    @Test
    public void shouldNotIncrementSharedCounterForNonExistingSubscription() {
        //given
        TopicName topicName = TopicName.fromQualifiedName("test.topic");
        String subscriptionName = "sub";
        doThrow(new SubscriptionNotExistsException(topicName, subscriptionName))
                .when(subscriptionRepository).ensureSubscriptionExists(topicName, subscriptionName);

        //when
        storage.setSubscriptionDeliveredCounter(topicName, subscriptionName, 1L);

        //then
        Mockito.verifyNoInteractions(sharedCounter);
    }
}