package pl.allegro.tech.hermes.frontend.buffer;

import com.codahale.metrics.Timer;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import java.io.File;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class BackupMessagesLoaderTest {

    private static final int ENTRIES = 100;
    private static final int AVERAGE_MESSAGE_SIZE = 600;

    private BrokerMessageProducer producer = mock(BrokerMessageProducer.class);

    private BrokerListeners listeners = mock(BrokerListeners.class);

    private TopicsCache topicsCache = mock(TopicsCache.class);

    private Trackers trackers = mock(Trackers.class);

    private ConfigFactory configFactory = mock(ConfigFactory.class);

    private CachedTopic cachedTopic = mock(CachedTopic.class);

    private File tempDir;

    private final Topic topic = TopicBuilder.topic("pl.allegro.tech.hermes.test").build();

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDir();

        when(cachedTopic.getTopic()).thenReturn(topic);
        when(cachedTopic.startBrokerLatencyTimers()).thenReturn(new StartedTimersPair(new Timer(), new Timer()));
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
        when(configFactory.getIntProperty(Configs.MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE)).thenReturn(5);
        when(configFactory.getIntProperty(Configs.MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS)).thenReturn(8);
        when(configFactory.getIntProperty(Configs.MESSAGES_LOCAL_STORAGE_MAX_RESEND_RETRIES)).thenReturn(2);

        MessageRepository messageRepository = ChronicleMapMessageRepository.create(
                new File(tempDir.getAbsoluteFile(), "messages.dat"), ENTRIES, AVERAGE_MESSAGE_SIZE);
        BackupMessagesLoader backupMessagesLoader = new BackupMessagesLoader(producer, listeners, topicsCache, trackers, configFactory);

        messageRepository.save(messageOfAge(1), topic);
        messageRepository.save(messageOfAge(10), topic);
        messageRepository.save(messageOfAge(10), topic);

        //when
        backupMessagesLoader.loadMessages(messageRepository);

        //then
        verify(producer, times(1)).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));
    }

    @Test
    public void shouldSendAndResendMessages() {
        //given
        int noOfSentCalls = 2;
        when(configFactory.getIntProperty(Configs.MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE)).thenReturn(5);
        when(configFactory.getIntProperty(Configs.MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS)).thenReturn(8);
        when(configFactory.getIntProperty(Configs.MESSAGES_LOCAL_STORAGE_MAX_RESEND_RETRIES)).thenReturn(noOfSentCalls - 1);
        when(trackers.get(eq(topic))).thenReturn(new NoOperationPublishingTracker());

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((PublishingCallback) args[2]).onUnpublished((Message) args[0], ((CachedTopic) args[1]).getTopic(), new Exception("test"));
            return "";
        }).when(producer).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));


        MessageRepository messageRepository = ChronicleMapMessageRepository.create(
                new File(tempDir.getAbsoluteFile(), "messages.dat"), ENTRIES, AVERAGE_MESSAGE_SIZE);
        BackupMessagesLoader backupMessagesLoader = new BackupMessagesLoader(producer, listeners, topicsCache, trackers, configFactory);

        messageRepository.save(messageOfAge(1), topic);

        //when
        backupMessagesLoader.loadMessages(messageRepository);

        //then
        verify(producer, times(noOfSentCalls)).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));
        verify(listeners, times(noOfSentCalls)).onError(any(JsonMessage.class), eq(topic), any(Exception.class));
    }

    @Test
    public void shouldSendOnlyWhenBrokerTopicIsAvailable() {
        // given
        when(configFactory.getIntProperty(Configs.MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE)).thenReturn(1);
        when(configFactory.getIntProperty(Configs.MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS)).thenReturn(10);

        when(producer.isTopicAvailable(cachedTopic)).thenReturn(false).thenReturn(false).thenReturn(true);

        BackupMessagesLoader backupMessagesLoader = new BackupMessagesLoader(producer, listeners, topicsCache, trackers, configFactory);
        MessageRepository messageRepository = ChronicleMapMessageRepository.create(
                new File(tempDir.getAbsoluteFile(), "messages.dat"), ENTRIES, AVERAGE_MESSAGE_SIZE);
        messageRepository.save(messageOfAge(1), topic);

        // when
        backupMessagesLoader.loadMessages(messageRepository);

        // then
        verify(producer, times(3)).isTopicAvailable(cachedTopic);
        verify(producer, times(1)).send(any(JsonMessage.class), eq(cachedTopic), any(PublishingCallback.class));
    }

    private Message messageOfAge(int ageHours) {
        return new JsonMessage(
                MessageIdGenerator.generate(),
                "{'a':'b'}".getBytes(),
                now().minusHours(ageHours).toInstant(UTC).toEpochMilli());
    }
}