package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.message.wrapper.MessageMetadata;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.common.time.SystemClock;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;

import java.util.List;
import java.util.Map;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@RunWith(MockitoJUnitRunner.class)
public class KafkaMessageReceiverTest {

    private static final Topic TOPIC = topic().withContentType(Topic.ContentType.JSON).withName("group.topic1").build();
    private static final String CONTENT = "{\"test\":\"a\"}";
    private static final MessageMetadata METADATA = new MessageMetadata(1L, "unique");
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

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        Map<String, List<kafka.consumer.KafkaStream<byte[], byte[]>>> consumerMap = Maps.newHashMap();
        consumerMap.put(TOPIC.getQualifiedName(), ImmutableList.of(kafkaStream).asList());
        when(consumerConnector.createMessageStreams(any(Map.class))).thenReturn(consumerMap);
        when(messageContentWrapper.unwrap(WRAPPED_MESSAGE_CONTENT.getBytes(), TOPIC)).thenReturn(new UnwrappedMessageContent(METADATA, CONTENT.getBytes()));
    }

    @Test
    public void shouldReadMessage() {
        // given
        when(kafkaStream.iterator().next().message()).thenReturn(WRAPPED_MESSAGE_CONTENT.getBytes());
        KafkaMessageReceiver kafkaMsgReceiver = getKafkaMessageReceiver();

        // when
        Message msg = kafkaMsgReceiver.next();

        // then
        assertThat(new String(msg.getData())).isEqualTo(CONTENT);
    }

    @Test
    public void shouldThrowTimeoutException() {
        // given
        when(kafkaStream.iterator().next()).thenThrow(new ConsumerTimeoutException());
        KafkaMessageReceiver kafkaMsgReceiver = getKafkaMessageReceiver();

        // when
        catchException(kafkaMsgReceiver).next();

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(MessageReceivingTimeoutException.class);
    }

    @Test
    public void shouldRethrowIfOtherError() {
        // given
        when(kafkaStream.iterator().next()).thenThrow(new NullPointerException());
        KafkaMessageReceiver kafkaMsgReceiver = getKafkaMessageReceiver();

        // when
        catchException(kafkaMsgReceiver).next();

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(InternalProcessingException.class);
    }

    private KafkaMessageReceiver getKafkaMessageReceiver() {
        return new KafkaMessageReceiver(TOPIC, consumerConnector, new ConfigFactory(),
                messageContentWrapper, timer, new SystemClock());
    }

}
