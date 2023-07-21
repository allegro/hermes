package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Tag;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Set;

class SubscriptionTagsFactory {

    static Set<Tag> subscriptionTags(SubscriptionName subscriptionName) {
        return Set.of(
                Tag.of("group", subscriptionName.getTopicName().getGroupName()),
                Tag.of("topic", subscriptionName.getTopicName().getName()),
                Tag.of("subscription", subscriptionName.getName())
        );
    }
}
