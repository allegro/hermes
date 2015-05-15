package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.Map;

public class DisabledAsyncOffsetMonitor implements AsyncOffsetMonitor {

    @Override
    public void process(Map<Subscription, PartitionOffset> offsetsPerSubscription) {
        //do nothing
    }

}
