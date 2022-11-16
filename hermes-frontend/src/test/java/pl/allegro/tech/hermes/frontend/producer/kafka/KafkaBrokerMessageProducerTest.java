package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.config.KafkaHeaderNameProperties;
import pl.allegro.tech.hermes.frontend.config.KafkaProducerProperties;
import pl.allegro.tech.hermes.frontend.config.SchemaProperties;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Charsets.UTF_8;
import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@RunWith(MockitoJUnitRunner.class)
public class KafkaBrokerMessageProducerTest {

    private static final Long TIMESTAMP = 1L;
    private static final String PARTITION_KEY = "partition-key";
    private static final String MESSAGE_ID = "id";
    private static final Topic TOPIC = topic("group.topic").build();
    private static final byte[] CONTENT = "{\"data\":\"json\"}".getBytes(UTF_8);
    private static final Message MESSAGE = new JsonMessage(MESSAGE_ID, CONTENT, TIMESTAMP, PARTITION_KEY);

    private final ByteArraySerializer serializer = new ByteArraySerializer();
    private final MockProducer<byte[], byte[]> leaderConfirmsProducer = new MockProducer<>(true, serializer, serializer);
    private final MockProducer<byte[], byte[]> everyoneConfirmProducer = new MockProducer<>(true, serializer, serializer);
    private final KafkaHeaderNameProperties kafkaHeaderNameProperties = new KafkaHeaderNameProperties();
    private final KafkaProducerProperties kafkaProducerProperties = new KafkaProducerProperties();
    private final Producers producers =
        new Producers(leaderConfirmsProducer, everyoneConfirmProducer, kafkaProducerProperties.isReportNodeMetricsEnabled());

    private KafkaBrokerMessageProducer producer;
    private final KafkaNamesMapper kafkaNamesMapper = new NamespaceKafkaNamesMapper("ns", "_");
    private final KafkaHeaderFactory kafkaHeaderFactory = new KafkaHeaderFactory(kafkaHeaderNameProperties);

    @Mock
    private HermesMetrics hermesMetrics;

    @Mock
    private KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher;

    private CachedTopic cachedTopic;

    private final SchemaProperties schemaProperties = new SchemaProperties();

    @Before
    public void before() {
        cachedTopic = new CachedTopic(TOPIC, hermesMetrics, kafkaNamesMapper.toKafkaTopics(TOPIC));
        MessageToKafkaProducerRecordConverter messageConverter =
            new MessageToKafkaProducerRecordConverter(kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
        producer = new KafkaBrokerMessageProducer(producers, kafkaTopicMetadataFetcher, hermesMetrics, messageConverter);
    }

    @After
    public void after() {
        leaderConfirmsProducer.clear();
        everyoneConfirmProducer.clear();
    }

    @Test
    public void shouldPublishOnTopicUsingKafkaTopicName() {
        //when
        producer.send(MESSAGE, cachedTopic, new DoNothing());

        //then
        List<ProducerRecord<byte[], byte[]>> records = leaderConfirmsProducer.history();
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).topic()).isEqualTo("ns_group.topic");
    }

    @Test
    public void shouldPublishOnTheSamePartition() {
        //when
        producer.send(MESSAGE, cachedTopic, new DoNothing());
        producer.send(MESSAGE, cachedTopic, new DoNothing());
        producer.send(MESSAGE, cachedTopic, new DoNothing());

        //then
        List<ProducerRecord<byte[], byte[]>> records = leaderConfirmsProducer.history();
        assertThat(records.size()).isEqualTo(3);
        assertThat(records.stream().filter(record -> PARTITION_KEY.equals(new String(record.key()))).count()).isEqualTo(3);
    }

    @Test
    public void shouldCallCallbackOnSend() {
        //given
        final AtomicBoolean callbackCalled = new AtomicBoolean(false);

        //when
        producer.send(MESSAGE, cachedTopic, new PublishingCallback() {
            @Override
            public void onUnpublished(Message message, Topic topic, Exception exception) {
                callbackCalled.set(true);
            }

            @Override
            public void onPublished(Message message, Topic topic) {
                callbackCalled.set(true);
            }
        });

        //then
        await().until(callbackCalled::get);
    }

    @Test
    public void shouldUseEveryoneConfirmProducerForTopicWithAckAll() {
        //given
        Topic topic = topic("group.all").withAck(Topic.Ack.ALL).build();
        CachedTopic cachedTopic = new CachedTopic(topic, hermesMetrics, kafkaNamesMapper.toKafkaTopics(topic));

        //when
        producer.send(MESSAGE, cachedTopic, new DoNothing());

        //then
        List<ProducerRecord<byte[], byte[]>> records = everyoneConfirmProducer.history();
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).topic()).isEqualTo("ns_group.all");
    }

    private static class DoNothing implements PublishingCallback {
        public void onUnpublished(Message message, Topic topic, Exception exception) {
        }

        public void onPublished(Message message, Topic topic) {
        }
    }

}
