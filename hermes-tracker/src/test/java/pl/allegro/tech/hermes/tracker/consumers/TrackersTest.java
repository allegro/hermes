package pl.allegro.tech.hermes.tracker.consumers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.TrackingMode;

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
        assertThat(trackers.get(subscription("group.topic", "sub").withTrackingMode(TrackingMode.TRACKING_OFF)
                .build())).isEqualTo(noOperationDeliveryTracker);
        assertThat(trackers.get(subscription("group.topic", "sub")
                .withTrackingMode(TrackingMode.TRACK_DISCARDED_ONLY).build())).isEqualTo(discardedSendingTracker);


        assertThat(trackers.get(subscription("group.topic", "sub").withTrackingMode(TrackingMode.TRACK_ALL)
                .build())).isEqualTo(messageDeliveryTracker);
    }

}