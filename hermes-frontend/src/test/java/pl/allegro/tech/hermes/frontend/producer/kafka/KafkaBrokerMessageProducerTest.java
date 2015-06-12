package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapperProvider;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.publishing.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@RunWith(MockitoJUnitRunner.class)
public class KafkaBrokerMessageProducerTest {

    private static final Long TIMESTAMP = 1L;
    private static final String MESSAGE_ID = "id";
    private static final String CONTENT_ROOT = "message";
    private static final String METADATA_ROOT = "metadata";
    private static final Topic TOPIC = topic().withName("group.topic").build();
    private static final byte[] CONTENT = "{\"data\":\"json\"}".getBytes(UTF_8);
    private static final Message MESSAGE = new Message(MESSAGE_ID, CONTENT, TIMESTAMP);

    private MockProducer leaderConfirmsProducer = new MockProducer();
    private MockProducer everyoneConfirmProducer = new MockProducer();
    private Producers producers = new Producers(leaderConfirmsProducer, everyoneConfirmProducer);

    private KafkaBrokerMessageProducer producer;
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private HermesMetrics hermesMetrics;

    @Before
    public void before() {
        JsonMessageContentWrapper jsonContentWrapper = new JsonMessageContentWrapper(CONTENT_ROOT, METADATA_ROOT, mapper);
        MessageContentWrapperProvider contentWrapperProvider = new MessageContentWrapperProvider(jsonContentWrapper, null);

        producer = new KafkaBrokerMessageProducer(producers, contentWrapperProvider, hermesMetrics);
    }

    @After
    public void after() {
        leaderConfirmsProducer.clear();
        everyoneConfirmProducer.clear();
    }

    @Test
    public void shouldCallCallbackOnSend() throws InterruptedException {
        //given
        final CountDownLatch latch = new CountDownLatch(1);

        //when
        producer.send(MESSAGE, TOPIC, new PublishingCallback() {
            @Override
            public void onUnpublished(Exception exception) {
                latch.countDown();
            }

            @Override
            public void onPublished(Message message, Topic topic) {
                latch.countDown();
            }
        });

        //then
        List<ProducerRecord<byte[], byte[]>> records = leaderConfirmsProducer.history();
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0)).isEqualToComparingFieldByField(new ProducerRecord<>("group.topic", CONTENT));

        latch.await();
    }

    @Test
    public void shouldWrapMessageWithMetadata() throws IOException {
        //given
        MapEntry content = entry(CONTENT_ROOT, readMap(CONTENT));

        //when
        producer.send(MESSAGE, TOPIC, new DoNothing());

        //then
        assertThat(firstMessage(leaderConfirmsProducer)).containsKey(METADATA_ROOT).contains(content);
        assertThat(firstMessageMetadata(leaderConfirmsProducer)).containsKey(MESSAGE_ID).containsEntry("timestamp", TIMESTAMP.intValue());
    }

    @Test
    public void shouldUseEveryoneConfirmProducerForTopicWithAckAll() {
        //given
        Topic topic = topic().withName("group.all").withAck(Topic.Ack.ALL).build();

        //when
        producer.send(MESSAGE, topic, new DoNothing());

        //then
        List<ProducerRecord<byte [], byte[]>> records = everyoneConfirmProducer.history();
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0)).isEqualToComparingFieldByField(new ProducerRecord<>("group.all", CONTENT));
    }

    private Map<String, Object> firstMessage(MockProducer producer) throws IOException {
        return readMap(producer.history().get(0).value());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstMessageMetadata(MockProducer producer) throws IOException {
        return (Map<String, Object>) firstMessage(producer).get(METADATA_ROOT);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap(byte[] result) throws IOException {
        return mapper.readValue(new String(result), Map.class);
    }

    private static class DoNothing implements PublishingCallback {
        public void onUnpublished(Exception exception) {
        }

        public void onPublished(Message message, Topic topic) {
        }
    }

}
