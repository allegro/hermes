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

    @InjectMocks
    Trackers trackers;

    @Test
    public void shouldDispatchCorrectTracker() {
        assertThat(trackers.get(subscription("group.topic", "sub").withTrackingEnabled(false).build())).isEqualTo(noOperationDeliveryTracker);
        assertThat(trackers.get(subscription("group.topic", "sub").withTrackingEnabled(true).build())).isEqualTo(messageDeliveryTracker);
    }

}