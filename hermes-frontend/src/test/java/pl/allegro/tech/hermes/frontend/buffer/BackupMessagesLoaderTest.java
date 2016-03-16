package pl.allegro.tech.hermes.frontend.buffer;

import com.codahale.metrics.Timer;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class BackupMessagesLoaderTest {

    private BrokerMessageProducer producer = mock(BrokerMessageProducer.class);

    private HermesMetrics metrics = mock(HermesMetrics.class);

    private BrokerListeners listeners = mock(BrokerListeners.class);

    private TopicsCache topicsCache = mock(TopicsCache.class);

    private Trackers trackers = mock(Trackers.class);

    private ConfigFactory configFactory = mock(ConfigFactory.class);

    private File tempDir;

    private final TopicName topicName = TopicName.fromQualifiedName("pl.allegro.tech.hermes.test");
    private final Topic topic = TopicBuilder.topic(topicName).build();

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDir();
        when(topicsCache.getTopic(topicName)).thenReturn(Optional.of(topic));
        when(metrics.timer(anyString())).thenReturn(new Timer());
        when(metrics.timer(anyString(), eq(topicName))).thenReturn(new Timer());
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

        MessageRepository messageRepository = new ChronicleMapMessageRepository(new File(tempDir.getAbsoluteFile(), "messages.dat"));
        BackupMessagesLoader backupMessagesLoader = new BackupMessagesLoader(producer, metrics,listeners, topicsCache, trackers, configFactory);

        messageRepository.save(messageOfAge(1), topic);
        messageRepository.save(messageOfAge(10), topic);
        messageRepository.save(messageOfAge(10), topic);

        //when
        backupMessagesLoader.loadMessages(messageRepository);

        //then
        verify(producer, times(1)).send(any(JsonMessage.class), eq(topic), any(PublishingCallback.class));
    }

    private Message messageOfAge(int ageHours) {
        return new JsonMessage(UUID.randomUUID().toString(), "{'a':'b'}".getBytes(), now().minusHours(ageHours).toInstant(UTC).toEpochMilli());
    }
}