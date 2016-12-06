package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class ChronicleMapMessageRepositoryTest {

    private static final int ENTRIES = 100;
    private static final int AVERAGE_MESSAGE_SIZE = 600;

    private File file;
    private MessageRepository messageRepository;

    @Before
    public void setUp() throws Throwable {
        file = File.createTempFile("local_backup", ".dat");
        messageRepository = ChronicleMapMessageRepository.create(file, ENTRIES, AVERAGE_MESSAGE_SIZE);
    }

    @Test
    public void shouldSaveFindAndDeleteMessage() throws Exception {
        //given
        byte[] messageContent = "hello world".getBytes();
        long timestamp = System.currentTimeMillis();
        String id = MessageIdGenerator.generate();
        Message message = new JsonMessage(id, messageContent, timestamp);
        String qualifiedName = "groupName.topic";

        Topic topic = topic(qualifiedName).build();

        //when
        messageRepository.save(message, topic);

        //then
        assertThat(messageRepository.findAll()).contains(new BackupMessage(id, messageContent, timestamp, qualifiedName));

        //when
        messageRepository.delete(id);

        //then
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    public void shouldSaveMultipleTimesFindAndDeleteMessage() throws Exception {
        //given
        byte[] messageContent = "hello world".getBytes();
        long timestamp = System.currentTimeMillis();
        String id1 = MessageIdGenerator.generate();
        Message message1 = new JsonMessage(id1, messageContent, timestamp);
        String id2 = MessageIdGenerator.generate();
        Message message2 = new JsonMessage(id2, messageContent, timestamp);
        String qualifiedName = "groupName.topic";

        Topic topic = topic(qualifiedName).build();

        //when
        messageRepository.save(message1, topic);
        messageRepository.save(message2, topic);
        messageRepository.save(message1, topic);
        messageRepository.save(message2, topic);

        //then
        assertThat(messageRepository.findAll()).contains(new BackupMessage(id1, messageContent, timestamp, qualifiedName));

        //when
        messageRepository.delete(id1);

        //then
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).contains(new BackupMessage(id2, messageContent, timestamp, qualifiedName));
    }

    @Test
    public void shouldCreateRepositoryFromFile() {
        //given
        String baseDir = Files.createTempDir().getAbsolutePath();
        File file = new File(baseDir, "messages.dat");

        //when
        ChronicleMapMessageRepository.create(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

        //then
        assertThat(file).exists();
    }
}