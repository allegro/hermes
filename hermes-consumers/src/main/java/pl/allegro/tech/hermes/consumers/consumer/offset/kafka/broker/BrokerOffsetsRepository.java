package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Lists;
import kafka.common.ErrorMapping;
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
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.FailedToCommitOffsets;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.CannotCommitOffsetToBrokerException;

import javax.inject.Inject;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BrokerOffsetsRepository {

    private static final Logger logger = LoggerFactory.getLogger(BrokerOffsetsRepository.class);

    private static final String EMPTY_METADATA = "";
    private static final int CORRELATION_ID = 0;
    private static final short VERSION_ID = 1; // version 1 and above commit to Kafka, version 0 commits to ZooKeeper

    private final BlockingChannelFactory blockingChannelFactory;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final Clock clock;
    private final HermesMetrics metrics;

    private final LoadingCache<SubscriptionName, BlockingChannel> channels;
    private final String clientId;

    @Inject
    public BrokerOffsetsRepository(BlockingChannelFactory blockingChannelFactory,
                                   KafkaNamesMapper kafkaNamesMapper,
                                   Clock clock,
                                   HostnameResolver hostnameResolver,
                                   HermesMetrics hermesMetrics,
                                   ConfigFactory configFactory) {
        this(
                blockingChannelFactory,
                configFactory.getIntProperty(Configs.KAFKA_CONSUMER_OFFSET_COMMITTER_BROKER_CONNECTION_EXPIRATION),
                kafkaNamesMapper,
                clock,
                hostnameResolver,
                hermesMetrics
        );
    }

    public BrokerOffsetsRepository(BlockingChannelFactory blockingChannelFactory,
                                   int channelCacheExpiryTimeSeconds,
                                   KafkaNamesMapper kafkaNamesMapper,
                                   Clock clock,
                                   HostnameResolver hostnameResolver,
                                   HermesMetrics hermesMetrics) {
        this.blockingChannelFactory = blockingChannelFactory;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.metrics = hermesMetrics;
        this.clientId = clientId(hostnameResolver);

        channels = CacheBuilder.newBuilder()
                .expireAfterAccess(channelCacheExpiryTimeSeconds, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<SubscriptionName, BlockingChannel> notification) -> notification.getValue().disconnect())
                .build(new CacheLoader<SubscriptionName, BlockingChannel>() {
                    public BlockingChannel load(SubscriptionName key) {
                        BlockingChannel channel = blockingChannelFactory.create(kafkaNamesMapper.toConsumerGroupId(key));
                        channel.connect();
                        return channel;
                    }
                });
    }


    private String clientId(HostnameResolver hostnameResolver) {
        return hostnameResolver.resolve() + "_" + UUID.randomUUID();
    }

    public FailedToCommitOffsets commit(OffsetsToCommit offsetsToCommit) {
        FailedToCommitOffsets failedOffsets = new FailedToCommitOffsets();
        for (SubscriptionName subscriptionName : offsetsToCommit.subscriptionNames()) {
            Set<SubscriptionPartitionOffset> subscriptionOffsets = offsetsToCommit.batchFor(subscriptionName);

            try(Timer.Context c = metrics.timer("offset-committer.single-commit.kafka").time()) {
                commit(subscriptionName, subscriptionOffsets);
            } catch (Exception exception) {
                logger.warn("Failed to commit offsets for subscription {}", subscriptionName, exception);
                failedOffsets.add(subscriptionOffsets);
            }
        }

        return failedOffsets;
    }

    private void commit(SubscriptionName subscriptionName, Set<SubscriptionPartitionOffset> subscriptionPartitionOffsets) {
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscriptionName);
        OffsetCommitRequest commitRequest = createCommitRequest(consumerGroupId, subscriptionPartitionOffsets);
        OffsetCommitResponse commitResponse;
        try {
            commitResponse = commitOffset(subscriptionName, commitRequest);
        } catch (Exception exception) {
            channels.invalidate(subscriptionName);
            throw new CannotCommitOffsetToBrokerException(subscriptionName, exception);
        }
        if (commitResponse.hasError()) {
            commitResponse.errors().values()
                    .stream()
                    .map(error -> (Short) error)
                    .filter(error -> error == ErrorMapping.NotCoordinatorForConsumerCode() || error == ErrorMapping.ConsumerCoordinatorNotAvailableCode())
                    .findAny()
                    .ifPresent(error -> channels.invalidate(subscriptionName));

            throw new CannotCommitOffsetToBrokerException(subscriptionName, new BrokerOffsetCommitErrors(commitResponse.errors()));
        }
    }

    private OffsetCommitRequest createCommitRequest(ConsumerGroupId consumerGroupId, Set<SubscriptionPartitionOffset> offsets) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = createOffset(offsets);

        return new OffsetCommitRequest(
                consumerGroupId.asString(),
                offset,
                CORRELATION_ID,
                clientId,
                VERSION_ID
        );
    }

    private Map<TopicAndPartition, OffsetAndMetadata> createOffset(Set<SubscriptionPartitionOffset> partitionOffsets) {
        Map<TopicAndPartition, OffsetAndMetadata> offsetsData = new LinkedHashMap<>();

        for (SubscriptionPartitionOffset partitionOffset : partitionOffsets) {
            TopicAndPartition topicAndPartition = new TopicAndPartition(partitionOffset.getKafkaTopicName().asString(), partitionOffset.getPartition());
            offsetsData.put(topicAndPartition, new OffsetAndMetadata(
                    partitionOffset.getOffset(),
                    EMPTY_METADATA,
                    clock.millis())
            );
        }
        return offsetsData;
    }

    private OffsetCommitResponse commitOffset(SubscriptionName subscription, OffsetCommitRequest commitRequest) throws ExecutionException {
        BlockingChannel channel = channels.get(subscription);
        channel.send(commitRequest.underlying());
        return OffsetCommitResponse.readFrom(channel.receive().buffer());
    }

    public void moveOffset(SubscriptionPartitionOffset subscriptionPartitionOffset) {
        long currentOffset = findOffset(subscriptionPartitionOffset.getSubscriptionPartition());

        if (currentOffset == -1 || currentOffset > subscriptionPartitionOffset.getOffset()) {
            commit(subscriptionPartitionOffset.getSubscriptionName(), Sets.newHashSet(subscriptionPartitionOffset));
        } else {
            logger.warn("Tried to move offset for subscription {} and partition {} to {} which is in the future. Current offset: {}",
                    subscriptionPartitionOffset.getSubscriptionName(),
                    subscriptionPartitionOffset.getPartition(),
                    subscriptionPartitionOffset.getOffset(),
                    currentOffset);
        }
    }

    public long findOffset(SubscriptionPartition subscriptionPartition) {
        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscriptionPartition.getSubscriptionName());
        BlockingChannel channel = blockingChannelFactory.create(groupId);
        try {
            channel.connect();
            TopicAndPartition topicAndPartition = new TopicAndPartition(
                    subscriptionPartition.getKafkaTopicName().asString(), subscriptionPartition.getPartition()
            );
            List<TopicAndPartition> partitions = Lists.newArrayList(topicAndPartition);

            OffsetFetchRequest fetchRequest = new OffsetFetchRequest(
                    groupId.asString(),
                    partitions,
                    VERSION_ID,
                    CORRELATION_ID,
                    clientId);

            channel.send(fetchRequest.underlying());
            OffsetFetchResponse fetchResponse = OffsetFetchResponse.readFrom(channel.receive().buffer());
            Map<TopicAndPartition, OffsetMetadataAndError> result = fetchResponse.offsets();
            OffsetMetadataAndError offset = result.get(topicAndPartition);

            return offset.offset();
        } finally {
            channel.disconnect();
        }
    }
}
