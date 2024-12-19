package pl.allegro.tech.hermes.tracker.management;

import java.util.Collection;

public interface TrackingUrlProvider {
  Collection<TrackingUrl> getTrackingUrlsForTopic(String qualifiedTopicName);

  Collection<TrackingUrl> getTrackingUrlsForSubscription(
      String qualifiedTopicName, String subscriptionName);
}
