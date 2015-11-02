package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Charsets.UTF_8;
import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@RunWith(MockitoJUnitRunner.class)
public class KafkaBrokerMessageProducerTest {

    private static final Long TIMESTAMP = 1L;
    private static final String MESSAGE_ID = "id";
    private static final String TRACE_ID = UUID.randomUUID().toString();
    private static final Topic TOPIC = topic().applyDefaults().withName("group.topic").build();
    private static final byte[] CONTENT = "{\"data\":\"json\"}".getBytes(UTF_8);
    private static final Message MESSAGE = new Message(MESSAGE_ID, TRACE_ID, CONTENT, TIMESTAMP);

    private MockProducer leaderConfirmsProducer = new MockProducer();
    private MockProducer everyoneConfirmProducer = new MockProducer();
    private Producers producers = new Producers(leaderConfirmsProducer, everyoneConfirmProducer, new ConfigFactory());

    private KafkaBrokerMessageProducer producer;
    private KafkaNamesMapper kafkaNamesMapper = new KafkaNamesMapper("ns");

    @Mock
    private HermesMetrics hermesMetrics;

    @Before
    public void before() {
        producer = new KafkaBrokerMessageProducer(producers, hermesMetrics, kafkaNamesMapper);
    }

    @After
    public void after() {
        leaderConfirmsProducer.clear();
        everyoneConfirmProducer.clear();
    }

    @Test
    public void shouldPublishOnTopicUsingKafkaTopicName() throws InterruptedException {
        //when
        producer.send(MESSAGE, TOPIC, new DoNothing());

        //then
        List<ProducerRecord<byte[], byte[]>> records = leaderConfirmsProducer.history();
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).topic()).isEqualTo("ns_group.topic");
    }

    @Test
    public void shouldCallCallbackOnSend() throws InterruptedException {
        //given
        final AtomicBoolean callbackCalled = new AtomicBoolean(false);

        //when
        producer.send(MESSAGE, TOPIC, new PublishingCallback() {
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
        Topic topic = topic().applyDefaults().withName("group.all").withAck(Topic.Ack.ALL).build();

        //when
        producer.send(MESSAGE, topic, new DoNothing());

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
