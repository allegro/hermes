package pl.allegro.tech.hermes.consumers.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionSuspenderTest {
    public static final TopicName TOPIC_NAME = new TopicName("group", "topic0");
    public static final String SUBSCRIPTION_NAME = "subscription0";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Subscription subscription;

    @InjectMocks
    private SubscriptionSuspender subscriptionSuspender;

    @Before
    public void setUp() {
        when(subscription.getName()).thenReturn(SUBSCRIPTION_NAME);
        when(subscription.getTopicName()).thenReturn(TOPIC_NAME);
        when(subscription.getState()).thenReturn(Subscription.State.ACTIVE);
        when(subscriptionRepository.getSubscriptionDetails(TOPIC_NAME, SUBSCRIPTION_NAME)).thenReturn(subscription);
    }

    @Test
    public void shouldSuspended() {
        //given && when
        subscriptionSuspender.suspend(subscription);

        //then
        verify(subscription).setState(Subscription.State.SUSPENDED);
        verify(subscriptionRepository).updateSubscription(subscription);
    }

    @Test
    public void shouldNotSuspendWhenAlreadySuspended() {
        //given
        Subscription newerSubscription = mock(Subscription.class);
        when(newerSubscription.getState()).thenReturn(Subscription.State.SUSPENDED);
        when(subscriptionRepository.getSubscriptionDetails(TOPIC_NAME, SUBSCRIPTION_NAME)).thenReturn(newerSubscription);

        //when
        subscriptionSuspender.suspend(subscription);

        //then
        verify(subscriptionRepository, never()).updateSubscription(any(Subscription.class));
    }
}
