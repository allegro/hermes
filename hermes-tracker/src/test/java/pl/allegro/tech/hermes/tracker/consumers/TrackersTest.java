package pl.allegro.tech.hermes.tracker.consumers;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.TrackingMode.TRACKING_OFF;
import static pl.allegro.tech.hermes.api.TrackingMode.TRACK_ALL;
import static pl.allegro.tech.hermes.api.TrackingMode.TRACK_DISCARDED_ONLY;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

import java.util.ArrayList;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TrackingMode;

public class TrackersTest {

  @Test
  public void shouldDispatchCorrectTracker() {
    // given
    Trackers trackers = new Trackers(new ArrayList<>());

    // when
    SendingTracker trackingOffTracker = trackers.get(subscriptionWithTrackingMode(TRACKING_OFF));
    SendingTracker trackDiscardedOnlyTracker =
        trackers.get(subscriptionWithTrackingMode(TRACK_DISCARDED_ONLY));
    SendingTracker trackAllTracker = trackers.get(subscriptionWithTrackingMode(TRACK_ALL));

    // then
    assertThat(trackingOffTracker.getClass()).isEqualTo(NoOperationSendingTracker.class);
    assertThat(trackDiscardedOnlyTracker.getClass()).isEqualTo(DiscardedSendingTracker.class);
    assertThat(trackAllTracker.getClass()).isEqualTo(SendingMessageTracker.class);
  }

  private static Subscription subscriptionWithTrackingMode(TrackingMode trackingMode) {
    return subscription("group.topic", "sub").withTrackingMode(trackingMode).build();
  }
}
