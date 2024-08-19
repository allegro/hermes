package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.io.Files;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.config.LocalMessageStorageProperties;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;
import pl.allegro.tech.hermes.schema.SchemaExistenceEnsurer;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BackupMessagesLoaderTest {

    private static final int ENTRIES = 100;
    private static final int AVERAGE_MESSAGE_SIZE = 600;

    private final BrokerMessageProducer producer = mock(BrokerMessageProducer.class);

    private final BrokerListeners listeners = mock(BrokerListeners.class);

    private final TopicsCache topicsCache = mock(TopicsCache.class);

    private final Trackers trackers = mock(Trackers.class);

    private final CachedTopic cachedTopic = mock(CachedTopic.class);

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);

    private final SchemaExistenceEnsurer schemaExistenceEnsurer = mock(SchemaExistenceEnsurer.class);

    private File tempDir;

    private final Topic topic = TopicBuilder.topic("pl.allegro.tech.hermes.test").build();

    @Before
    public void setUp() {
        tempDir = Files.createTempDir();

        Timer micrometerTimer = new SimpleMeterRegistry().timer("broker-latency");
        when(cachedTopic.getTopic()).thenReturn(topic);
        when(cachedTopic.startBrokerLatencyTimer()).thenReturn(HermesTimerContext.from(micrometerTimer));
        when(topicsCache.getTopic(topic.getQualifiedName())).thenReturn(Optional.of(cachedTopic));
        when(producer.isTopicAvailable(cachedTopic)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void shouldNotSendOldMessages() {
        //given
        LocalMessageStorageProperties localMessageStorageProperties = new LocalMessageStorageProperties();
        localMessageStorageProperties.setMaxAge(Duration.ofHours(8));
        localMessageStorageProperties.setMaxResendRetries(2);

        MessageRepository messageRepository = new ChronicleMapMessageRepository(
                new File(tempDir.getAbsoluteFile(), "messages.dat"),
                ENTRIES,
                AVERAGE_MESSAGE_SIZE
        );

        final BackupMessagesLoader backupMessagesLoader =
            new BackupMessagesLoader(
                producer,
                producer,
                listeners,
                topicsCache,
                schemaRepository,
                schemaExistenceEnsurer,
                trackers,
                localMessageStorageProperties
            );

        messageRepository.save(messageOfAge(1), topic);
        messageRepository.save(messageOfAge(10), topic);
        messageRepository.save(messageOfAge(10), topic);

        //when
        backupMessagesLoader.loadMessages(messageRepository.findAll());

        //then
        verify(producer, times(1)).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));
    }

    @Test
    public void shouldSendAndResendMessages() {
        //given
        int noOfSentCalls = 2;
        LocalMessageStorageProperties localMessageStorageProperties = new LocalMessageStorageProperties();
        localMessageStorageProperties.setMaxAge(Duration.ofHours(8));
        localMessageStorageProperties.setMaxResendRetries(noOfSentCalls - 1);
        when(trackers.get(eq(topic))).thenReturn(new NoOperationPublishingTracker());

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((PublishingCallback) args[2]).onUnpublished((Message) args[0], ((CachedTopic) args[1]).getTopic(), new Exception("test"));
            return "";
        }).when(producer).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));


        MessageRepository messageRepository = new ChronicleMapMessageRepository(
                new File(tempDir.getAbsoluteFile(), "messages.dat"),
                ENTRIES,
                AVERAGE_MESSAGE_SIZE
        );
        BackupMessagesLoader backupMessagesLoader =
            new BackupMessagesLoader(
                producer,
                producer,
                listeners,
                topicsCache,
                schemaRepository,
                schemaExistenceEnsurer,
                trackers,
                localMessageStorageProperties
            );

        messageRepository.save(messageOfAge(1), topic);

        //when
        backupMessagesLoader.loadMessages(messageRepository.findAll());

        //then
        verify(producer, times(noOfSentCalls)).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));
        verify(listeners, times(noOfSentCalls)).onError(any(JsonMessage.class), eq(topic), any(Exception.class));
    }

    @Test
    public void shouldSendOnlyWhenBrokerTopicIsAvailable() {
        // given
        LocalMessageStorageProperties localMessageStorageProperties = new LocalMessageStorageProperties();
        localMessageStorageProperties.setMaxAge(Duration.ofHours(8));

        when(producer.isTopicAvailable(cachedTopic)).thenReturn(false).thenReturn(false).thenReturn(true);

        BackupMessagesLoader backupMessagesLoader =
            new BackupMessagesLoader(
                producer,
                producer,
                listeners,
                topicsCache,
                schemaRepository,
                schemaExistenceEnsurer,
                trackers,
                localMessageStorageProperties
            );
        MessageRepository messageRepository = new ChronicleMapMessageRepository(
                new File(tempDir.getAbsoluteFile(), "messages.dat"),
                ENTRIES,
                AVERAGE_MESSAGE_SIZE
        );
        messageRepository.save(messageOfAge(1), topic);

        // when
        backupMessagesLoader.loadMessages(messageRepository.findAll());

        // then
        verify(producer, times(3)).isTopicAvailable(cachedTopic);
        verify(producer, times(1)).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));
    }

    @Test
    public void shouldSendMessageWithAllArgumentsFromBackupMessage() {
        //given
        LocalMessageStorageProperties localMessageStorageProperties = new LocalMessageStorageProperties();
        localMessageStorageProperties.setMaxAge(Duration.ofHours(8));
        localMessageStorageProperties.setMaxResendRetries(2);

        MessageRepository messageRepository = new ChronicleMapMessageRepository(
                new File(tempDir.getAbsoluteFile(), "messages.dat"),
                ENTRIES,
                AVERAGE_MESSAGE_SIZE
        );

        BackupMessagesLoader backupMessagesLoader =
            new BackupMessagesLoader(
                producer,
                producer,
                listeners,
                topicsCache,
                schemaRepository,
                schemaExistenceEnsurer,
                trackers,
                localMessageStorageProperties
            );

        messageRepository.save(messageOfAge(1), topic);

        //when
        List<BackupMessage> backupMessages = messageRepository.findAll();
        backupMessagesLoader.loadMessages(backupMessages);

        //then
        ArgumentCaptor<Message> messageArgument = ArgumentCaptor.forClass(Message.class);

        verify(producer, times(1)).send(messageArgument.capture(), eq(cachedTopic), any(PublishingCallback.class));
        Message sendMessage = messageArgument.getValue();
        assertThat(sendMessage.getPartitionKey()).isEqualTo(backupMessages.get(0).getPartitionKey());
        assertThat(sendMessage.getData()).isEqualTo(backupMessages.get(0).getData());
        assertThat(sendMessage.getId()).isEqualTo(backupMessages.get(0).getMessageId());
        assertThat(sendMessage.getTimestamp()).isEqualTo(backupMessages.get(0).getTimestamp());
        assertThat(sendMessage.getHTTPHeaders().get("propagated-http-header")).isEqualTo("example-value");
    }

    private Message messageOfAge(int ageHours) {
        return new JsonMessage(
                MessageIdGenerator.generate(),
                "{'a':'b'}".getBytes(),
                now().minusHours(ageHours).toInstant(UTC).toEpochMilli(),
                "partition-key",
                Map.of("propagated-http-header", "example-value")
        );
    }
}
