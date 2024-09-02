package pl.allegro.tech.hermes.frontend.producer.kafka;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.config.HTTPHeadersProperties;
import pl.allegro.tech.hermes.frontend.config.KafkaHeaderNameProperties;
import pl.allegro.tech.hermes.frontend.config.SchemaProperties;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

@RunWith(MockitoJUnitRunner.class)
public class LocalDatacenterMessageProducerTest {

  private static final Long TIMESTAMP = 1L;
  private static final String PARTITION_KEY = "partition-key";
  private static final String MESSAGE_ID = "id";
  private static final String datacenter = "dc";
  private static final Topic TOPIC = topic("group.topic").build();
  private static final byte[] CONTENT = "{\"data\":\"json\"}".getBytes(UTF_8);
  private static final Message MESSAGE =
      new JsonMessage(MESSAGE_ID, CONTENT, TIMESTAMP, PARTITION_KEY, emptyMap());

  private final ByteArraySerializer serializer = new ByteArraySerializer();
  private final MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry());

  private final BrokerLatencyReporter brokerLatencyReporter =
      new BrokerLatencyReporter(
          false, metricsFacade, Duration.ZERO, Executors.newSingleThreadExecutor());

  private final ScheduledExecutorService chaosScheduler =
      Executors.newSingleThreadScheduledExecutor();

  private final MockProducer<byte[], byte[]> leaderConfirmsProducer =
      new MockProducer<>(true, serializer, serializer);
  private final MockProducer<byte[], byte[]> everyoneConfirmProducer =
      new MockProducer<>(true, serializer, serializer);
  private final KafkaMessageSender<byte[], byte[]> leaderConfirmsProduceWrapper =
      new KafkaMessageSender<>(
          leaderConfirmsProducer, brokerLatencyReporter, metricsFacade, datacenter, chaosScheduler);
  private final KafkaMessageSender<byte[], byte[]> everyoneConfirmsProduceWrapper =
      new KafkaMessageSender<>(
          everyoneConfirmProducer,
          brokerLatencyReporter,
          metricsFacade,
          datacenter,
          chaosScheduler);

  private final KafkaHeaderNameProperties kafkaHeaderNameProperties =
      new KafkaHeaderNameProperties();
  private final HTTPHeadersPropagationAsKafkaHeadersProperties
      httpHeadersPropagationAsKafkaHeadersProperties =
          new HTTPHeadersProperties.PropagationAsKafkaHeadersProperties();
  @Mock private TopicsCache topicsCache;
  private final TopicMetadataLoadingExecutor topicMetadataLoadingExecutor =
      new TopicMetadataLoadingExecutor(topicsCache, 2, Duration.ofSeconds(10), 2);
  @Mock private AdminClient adminClient;
  private final MinInSyncReplicasLoader localMinInSyncReplicasLoader =
      new MinInSyncReplicasLoader(adminClient, Duration.ofMinutes(1));
  private final KafkaMessageSenders kafkaMessageSenders =
      new KafkaMessageSenders(
          topicMetadataLoadingExecutor,
          localMinInSyncReplicasLoader,
          new KafkaMessageSenders.Tuple(
              leaderConfirmsProduceWrapper, everyoneConfirmsProduceWrapper),
          emptyList());

  private LocalDatacenterMessageProducer producer;
  private final KafkaNamesMapper kafkaNamesMapper = new NamespaceKafkaNamesMapper("ns", "_");
  private final KafkaHeaderFactory kafkaHeaderFactory =
      new KafkaHeaderFactory(
          kafkaHeaderNameProperties, httpHeadersPropagationAsKafkaHeadersProperties);

  private CachedTopic cachedTopic;

  private final SchemaProperties schemaProperties = new SchemaProperties();

  @Mock private ThroughputRegistry throughputRegistry;

  @Before
  public void before() {
    cachedTopic =
        new CachedTopic(
            TOPIC, metricsFacade, throughputRegistry, kafkaNamesMapper.toKafkaTopics(TOPIC));
    MessageToKafkaProducerRecordConverter messageConverter =
        new MessageToKafkaProducerRecordConverter(
            kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
    producer = new LocalDatacenterMessageProducer(kafkaMessageSenders, messageConverter);
  }

  @After
  public void after() {
    leaderConfirmsProducer.clear();
    everyoneConfirmProducer.clear();
  }

  @Test
  public void shouldPublishOnTopicUsingKafkaTopicName() {
    // when
    producer.send(MESSAGE, cachedTopic, new DoNothing());

    // then
    List<ProducerRecord<byte[], byte[]>> records = leaderConfirmsProducer.history();
    assertThat(records.size()).isEqualTo(1);
    assertThat(records.get(0).topic()).isEqualTo("ns_group.topic");
  }

  @Test
  public void shouldPublishOnTheSamePartition() {
    // when
    producer.send(MESSAGE, cachedTopic, new DoNothing());
    producer.send(MESSAGE, cachedTopic, new DoNothing());
    producer.send(MESSAGE, cachedTopic, new DoNothing());

    // then
    List<ProducerRecord<byte[], byte[]>> records = leaderConfirmsProducer.history();
    assertThat(records.size()).isEqualTo(3);
    assertThat(
            records.stream()
                .filter(record -> PARTITION_KEY.equals(new String(record.key())))
                .count())
        .isEqualTo(3);
  }

  @Test
  public void shouldCallCallbackOnSend() {
    // given
    final AtomicBoolean callbackCalled = new AtomicBoolean(false);

    // when
    producer.send(
        MESSAGE,
        cachedTopic,
        new PublishingCallback() {
          @Override
          public void onUnpublished(Message message, Topic topic, Exception exception) {
            callbackCalled.set(true);
          }

          @Override
          public void onPublished(Message message, Topic topic) {
            callbackCalled.set(true);
          }

          @Override
          public void onEachPublished(Message message, Topic topic, String datacenter) {
            callbackCalled.set(true);
          }
        });

    // then
    await().until(callbackCalled::get);
  }

  @Test
  public void shouldUseEveryoneConfirmProducerForTopicWithAckAll() {
    // given
    Topic topic = topic("group.all").withAck(Topic.Ack.ALL).build();
    CachedTopic cachedTopic =
        new CachedTopic(
            topic, metricsFacade, throughputRegistry, kafkaNamesMapper.toKafkaTopics(topic));

    // when
    producer.send(MESSAGE, cachedTopic, new DoNothing());

    // then
    List<ProducerRecord<byte[], byte[]>> records = everyoneConfirmProducer.history();
    assertThat(records.size()).isEqualTo(1);
    assertThat(records.get(0).topic()).isEqualTo("ns_group.all");
  }

  private static class DoNothing implements PublishingCallback {
    public void onUnpublished(Message message, Topic topic, Exception exception) {}

    public void onPublished(Message message, Topic topic) {}

    @Override
    public void onEachPublished(Message message, Topic topic, String datacenter) {}
  }
}
