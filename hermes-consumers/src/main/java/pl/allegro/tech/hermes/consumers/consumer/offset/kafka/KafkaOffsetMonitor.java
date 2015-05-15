package pl.allegro.tech.hermes.consumers.consumer.offset.kafka;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import kafka.common.TopicAndPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.Map;
import java.util.Set;

public class KafkaOffsetMonitor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAsyncOffsetMonitor.class);

    private Set<TopicAndPartition> topicAndPartitions;
    private Map<Subscription, PartitionOffset> offsetsPerSubscription;
    private KafkaLatestOffsetReader kafkaLatestOffsetReader;
    private HermesMetrics hermesMetrics;
    private MetricsDeltaCalculator metricsDeltaCalculator;

    public KafkaOffsetMonitor(
            Map<Subscription, PartitionOffset> offsetsPerSubscription,
            KafkaLatestOffsetReader kafkaLatestOffsetReader,
            HermesMetrics hermesMetrics,
            MetricsDeltaCalculator metricsDeltaCalculator) {

        this.offsetsPerSubscription = offsetsPerSubscription;
        this.kafkaLatestOffsetReader = kafkaLatestOffsetReader;
        this.hermesMetrics = hermesMetrics;
        this.metricsDeltaCalculator = metricsDeltaCalculator;
        topicAndPartitions = Sets.newHashSet(
            Collections2.transform(offsetsPerSubscription.entrySet(), new TransformToTopicAndPartition())
        );
    }

    @Override
    public void run() {
        try {
            Map<TopicAndPartition, Long> latestOffsets = kafkaLatestOffsetReader.read(topicAndPartitions);

            for (Map.Entry<Subscription, PartitionOffset> entry: offsetsPerSubscription.entrySet()) {
                PartitionOffset partitionOffset = entry.getValue();
                markLagOffset(
                        latestOffsets.get(new TopicAndPartition(entry.getKey().getQualifiedTopicName(), partitionOffset.getPartition())),
                        entry.getKey(),
                        entry.getValue());
            }
        } catch (Exception e) {
            LOGGER.warn("Something went wrong while processing lag offset", e);
        }
    }

    private void markLagOffset(Long latestOffset, Subscription subscription, PartitionOffset partitionOffset) {
        if (latestOffset == null) {
            LOGGER.info("Didn't find latest offset for topic: {} and partition: {}",
                    subscription.getQualifiedTopicName(), partitionOffset.getPartition());
            return;
        }
        long lag = latestOffset - partitionOffset.getOffset();
        hermesMetrics.counterForOffsetLag(subscription, partitionOffset.getPartition()).inc(
            metricsDeltaCalculator.calculateDelta(subscription.getId() + "_" + partitionOffset.getPartition(), lag)
        );
    }

    private static final class TransformToTopicAndPartition
            implements Function<Map.Entry<Subscription, PartitionOffset>, TopicAndPartition> {
        @Override
        public TopicAndPartition apply(Map.Entry<Subscription, PartitionOffset> input) {
            return new TopicAndPartition(input.getKey().getQualifiedTopicName(), input.getValue().getPartition());
        }
    }
}
