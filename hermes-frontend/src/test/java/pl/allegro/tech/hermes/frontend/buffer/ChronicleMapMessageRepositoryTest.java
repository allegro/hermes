package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class ChronicleMapMessageRepositoryTest {

    private File file;
    private MessageRepository messageRepository;

    @Before
    public void setUp() throws Throwable {
        file = File.createTempFile("local_backup", ".dat");
        messageRepository = new ChronicleMapMessageRepository(file);
    }

    @Test
    public void shouldSaveFindAndDeleteMessage() throws Exception {
        //given
        byte[] messageContent = "hello world".getBytes();
        long timestamp = System.currentTimeMillis();
        String id = "id1";
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
    public void shouldCreateRepositoryFromFile() {
        //given
        String baseDir = Files.createTempDir().getAbsolutePath();
        File file = new File(baseDir, "messages.dat");

        //when
        new ChronicleMapMessageRepository(file);

        //then
        assertThat(file).exists();
    }
}