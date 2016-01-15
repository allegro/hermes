package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
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
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.CannotCommitOffsetToBrokerException;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import javax.inject.Inject;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BrokerOffsetsRepository {

    private static final String EMPTY_METADATA = "";
    private static final int CORRELATION_ID = 0;
    private static final short VERSION_ID = 1; // version 1 and above commit to Kafka, version 0 commits to ZooKeeper

    private final BlockingChannelFactory blockingChannelFactory;
    private final Clock clock;
    private final KafkaNamesMapper kafkaNamesMapper;

    private final LoadingCache<Subscription, BlockingChannel> channels;
    private final String clientId;

    @Inject
    public BrokerOffsetsRepository(BlockingChannelFactory blockingChannelFactory, Clock clock, HostnameResolver hostnameResolver,
                               ConfigFactory configFactory, KafkaNamesMapper kafkaNamesMapper) {
        this(blockingChannelFactory, clock, hostnameResolver, kafkaNamesMapper,
                configFactory.getIntProperty(Configs.KAFKA_CONSUMER_OFFSET_COMMITTER_BROKER_CONNECTION_EXPIRATION)
        );
    }

    public BrokerOffsetsRepository(BlockingChannelFactory blockingChannelFactory, Clock clock, HostnameResolver hostnameResolver,
                                   KafkaNamesMapper kafkaNamesMapper, int channelExpTime) {
        this.blockingChannelFactory = blockingChannelFactory;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.clientId = clientId(hostnameResolver);

        channels = CacheBuilder.newBuilder()
                .expireAfterAccess(channelExpTime, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<Subscription, BlockingChannel> notification) -> notification.getValue().disconnect())
                .build(new CacheLoader<Subscription, BlockingChannel>() {
                    public BlockingChannel load(Subscription key) {
                        BlockingChannel channel = blockingChannelFactory.create(kafkaNamesMapper.toConsumerGroupId(key));
                        channel.connect();
                        return channel;
                    }
                });
    }

    public void save(Subscription subscription, PartitionOffset partitionOffset) throws ExecutionException {
        OffsetCommitRequest commitRequest = createCommitRequest(subscription, partitionOffset);
        OffsetCommitResponse commitResponse;
        try {
            commitResponse = commitOffset(subscription, commitRequest);
        } catch (Exception e) {
            channels.invalidate(subscription);
            throw e;
        }
        if (commitResponse.hasError()) {
            commitResponse.errors().values()
                    .stream()
                    .map(error -> (Short) error)
                    .filter(error -> error == ErrorMapping.NotCoordinatorForConsumerCode() || error == ErrorMapping.ConsumerCoordinatorNotAvailableCode())
                    .findAny()
                    .ifPresent(error -> channels.invalidate(subscription));

            throw new CannotCommitOffsetToBrokerException(new BrokerOffsetCommitErrors(commitResponse.errors()));
        }
    }

    public void saveIfOffsetInThePast(Subscription subscription, PartitionOffset partitionOffset) throws ExecutionException {
        long currentOffset = find(subscription, partitionOffset.getTopic(), partitionOffset.getPartition());

        if (currentOffset == -1 || currentOffset > partitionOffset.getOffset()) {
            save(subscription, partitionOffset);
        }
    }

    private OffsetCommitResponse commitOffset(Subscription subscription, OffsetCommitRequest commitRequest) throws ExecutionException {
        BlockingChannel channel = channels.get(subscription);
        channel.send(commitRequest.underlying());
        return OffsetCommitResponse.readFrom(channel.receive().buffer());
    }

    private OffsetCommitRequest createCommitRequest(Subscription subscription, PartitionOffset partitionOffset) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = createOffset(partitionOffset);

        return new OffsetCommitRequest(
                kafkaNamesMapper.toConsumerGroupId(subscription).asString(),
                offset,
                CORRELATION_ID,
                clientId,
                VERSION_ID);
    }

    private Map<TopicAndPartition, OffsetAndMetadata> createOffset(PartitionOffset partitionOffset) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = new LinkedHashMap<>();
        TopicAndPartition topicAndPartition = new TopicAndPartition(partitionOffset.getTopic().asString(), partitionOffset.getPartition());
        offset.put(topicAndPartition, new OffsetAndMetadata(partitionOffset.getOffset(), EMPTY_METADATA, clock.millis()));
        return offset;
    }


    public long find(Subscription subscription, KafkaTopicName kafkaTopicName, int partitionId) {
        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription);
        BlockingChannel channel = blockingChannelFactory.create(groupId);
        channel.connect();

        TopicAndPartition topicAndPartition = new TopicAndPartition(kafkaTopicName.asString(), partitionId);
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
        channel.disconnect();
        return offset.offset();
    }

    private String clientId(HostnameResolver hostnameResolver) {
        return hostnameResolver.resolve() + "_" + UUID.randomUUID();
    }
}
