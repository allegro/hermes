package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Tag;
import java.util.Set;
import pl.allegro.tech.hermes.api.SubscriptionName;

class SubscriptionTagsFactory {

  static Set<Tag> subscriptionTags(SubscriptionName subscriptionName) {
    return Set.of(
        Tag.of("group", subscriptionName.getTopicName().getGroupName()),
        Tag.of("topic", subscriptionName.getTopicName().getName()),
        Tag.of("subscription", subscriptionName.getName()));
  }
}
