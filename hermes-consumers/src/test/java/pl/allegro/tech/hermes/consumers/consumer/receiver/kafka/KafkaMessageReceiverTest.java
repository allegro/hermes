package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageMetadata;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@RunWith(MockitoJUnitRunner.class)
public class KafkaMessageReceiverTest {

    private static final Topic TOPIC = topic("group.topic1").build();
    private static final Integer KAFKA_STREAM_COUNT = 1;
    private static final String CONTENT = "{\"test\":\"a\"}";
    private static final MessageMetadata METADATA = new MessageMetadata(1L, "unique", ImmutableMap.of());
    private static final String WRAPPED_MESSAGE_CONTENT =
            format("{\"_w\":true,\"metadata\":{\"id\":\"%s\",\"timestamp\":%d},\"%s\":%s}", METADATA.getId(), METADATA.getTimestamp(), "message", CONTENT);

    @Mock
    private MessageContentWrapper messageContentWrapper;

    @Mock
    private ConsumerConfig consumerConfig;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ConsumerConnector consumerConnector;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private KafkaStream<byte[], byte[]> kafkaStream;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Timer timer;

    @Mock
    private MessageAndMetadata<byte[], byte[]> messageAndMetadata;

    @Mock
    private SchemaRepository schemaRepository;

    private KafkaNamesMapper kafkaNamesMapper = new NamespaceKafkaNamesMapper("ns");

    private KafkaMessageReceiver kafkaMsgReceiver;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        Map<String, List<kafka.consumer.KafkaStream<byte[], byte[]>>> consumerMap = singletonMap("ns_group.topic1", ImmutableList.of(kafkaStream));
        Map<String, Integer> expectedTopicCountMap = singletonMap("ns_group.topic1", KAFKA_STREAM_COUNT);
        when(consumerConnector.createMessageStreams(expectedTopicCountMap)).thenReturn(consumerMap);
        when(messageAndMetadata.message()).thenReturn(WRAPPED_MESSAGE_CONTENT.getBytes());
        when(messageContentWrapper.unwrapJson(WRAPPED_MESSAGE_CONTENT.getBytes())).thenReturn(new UnwrappedMessageContent(METADATA, CONTENT.getBytes()));
        Subscription subscription = SubscriptionBuilder.subscription(SubscriptionName.fromString("ns_group.topic$sub")).build();
        kafkaMsgReceiver = new KafkaMessageReceiver(TOPIC, consumerConnector, messageContentWrapper,
                timer, Clock.systemDefaultZone(), kafkaNamesMapper, KAFKA_STREAM_COUNT, 100, subscription, schemaRepository);
    }

    @After
    public void cleanUp() {
        kafkaMsgReceiver.stop();
    }

    @Test
    public void shouldReadMessage() {
        // given
        when(kafkaStream.iterator().next()).thenReturn(messageAndMetadata);

        // when
        Optional<Message> message = kafkaMsgReceiver.next();

        // then
        assertThat(message).isPresent();
        assertThat(new String(message.get().getData())).isEqualTo(CONTENT);
    }

    @Test
    public void shouldReturnEmptyOptionalWhilePollingFromEmptyQueue() {
        // given
        when(kafkaStream.iterator().next()).thenThrow(new ConsumerTimeoutException());

        // when
        Optional<Message> message = kafkaMsgReceiver.next();

        // then
        assertThat(message).isEmpty();
    }

}
