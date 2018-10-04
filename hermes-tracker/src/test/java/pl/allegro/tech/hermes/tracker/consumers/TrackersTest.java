package pl.allegro.tech.hermes.tracker.consumers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class TrackersTest {

    @Mock
    SendingMessageTracker messageDeliveryTracker;

    @Mock
    NoOperationSendingTracker noOperationDeliveryTracker;

    @Mock
    DiscardedSendingTracker discardedSendingTracker;

    @InjectMocks
    Trackers trackers;

    @Test
    public void shouldDispatchCorrectTracker() {
        assertThat(trackers.get(subscription("group.topic", "sub").withFullTrackingEnabled(false)
                .build())).isEqualTo(noOperationDeliveryTracker);
        assertThat(trackers.get(subscription("group.topic", "sub").withFullTrackingEnabled(false)
                .withDiscardedTrackingEnabled(true).build())).isEqualTo(discardedSendingTracker);


        assertThat(trackers.get(subscription("group.topic", "sub").withFullTrackingEnabled(true)
                .build())).isEqualTo(messageDeliveryTracker);
        assertThat(trackers.get(subscription("group.topic", "sub").withFullTrackingEnabled(true)
                .withDiscardedTrackingEnabled(true).build())).isEqualTo(messageDeliveryTracker);

    }

}