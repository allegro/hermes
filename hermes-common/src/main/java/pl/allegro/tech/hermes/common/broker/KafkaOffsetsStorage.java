package pl.allegro.tech.hermes.common.broker;

import jersey.repackaged.com.google.common.collect.Lists;
import kafka.common.OffsetAndMetadata;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.network.BlockingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.time.Clock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class KafkaOffsetsStorage implements OffsetsStorage {

    private static final Logger logger = LoggerFactory.getLogger(KafkaOffsetsStorage.class);
    private static final String EMPTY_METADATA = "";
    private static final String CLIENT_ID = "client";
    private static final int CORRELATION_ID = 0;
    private static final short VERSION = 1;

    private final BlockingChannelFactory blockingChannelFactory;
    private final Clock clock;

    public KafkaOffsetsStorage(BlockingChannelFactory blockingChannelFactory, Clock clock) {
        this.blockingChannelFactory = blockingChannelFactory;
        this.clock = clock;
    }

    @Override
    public void setSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId, long offset) {
        String groupId = Subscription.getId(topicName, subscriptionName);
        BlockingChannel channel = blockingChannelFactory.create(groupId);

        channel.connect();

        OffsetCommitRequest commitRequest = createCommitRequest(topicName, groupId, partitionId, offset);
        channel.send(commitRequest.underlying());
        OffsetCommitResponse commitResponse = OffsetCommitResponse.readFrom(channel.receive().buffer());

        if (commitResponse.hasError()) {
            logger.error("Cannot commit offset, error codes: %s ", new BrokerOffsetCommitErrors(commitResponse.errors()).toString());
        }

        channel.disconnect();
    }

    @Override
    public long getSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId) {
        String groupId = Subscription.getId(topicName, subscriptionName);
        BlockingChannel channel = blockingChannelFactory.create(groupId);
        channel.connect();

        TopicAndPartition topicAndPartition = new TopicAndPartition(topicName.qualifiedName(), partitionId);
        List<TopicAndPartition> partitions = Lists.newArrayList(topicAndPartition);

        OffsetFetchRequest fetchRequest = new OffsetFetchRequest(
                groupId,
                partitions,
                VERSION,
                CORRELATION_ID,
                CLIENT_ID);

        channel.send(fetchRequest.underlying());
        OffsetFetchResponse fetchResponse = OffsetFetchResponse.readFrom(channel.receive().buffer());
        Map<TopicAndPartition, OffsetMetadataAndError> result = fetchResponse.offsets();
        OffsetMetadataAndError offset = result.get(topicAndPartition);
        channel.disconnect();
        return offset.offset();
    }

    private OffsetCommitRequest createCommitRequest(TopicName topicName, String groupId, int partition, long offset) {
        Map<TopicAndPartition, OffsetAndMetadata> offsetMap = createOffset(topicName, partition, offset);

        return new OffsetCommitRequest(groupId, offsetMap, CORRELATION_ID, CLIENT_ID, VERSION);
    }

    private Map<TopicAndPartition, OffsetAndMetadata> createOffset(TopicName topicName, int partition, long offset) {
        Map<TopicAndPartition, OffsetAndMetadata> offsetMap = new LinkedHashMap<>();
        TopicAndPartition topicAndPartition = new TopicAndPartition(topicName.qualifiedName(), partition);
        offsetMap.put(topicAndPartition, new OffsetAndMetadata(offset, EMPTY_METADATA, clock.getTime()));
        return offsetMap;
    }
}
