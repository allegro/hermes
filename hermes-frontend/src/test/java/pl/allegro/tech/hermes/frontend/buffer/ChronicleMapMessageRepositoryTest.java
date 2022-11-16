package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

import java.io.File;
import java.util.UUID;

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
        messageRepository = new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);
    }

    @Test
    public void shouldSaveFindAndDeleteMessage() {
        //given
        String qualifiedName = "groupName.topic";

        Message message = generateJsonMessage();
        String id = message.getId();
        byte[] messageContent = message.getData();
        long timestamp = message.getTimestamp();

        Topic topic = topic(qualifiedName).build();

        //when
        messageRepository.save(message, topic);

        //then
        assertThat(messageRepository.findAll()).contains(
                new BackupMessage(id, messageContent, timestamp, qualifiedName, message.getPartitionKey(), null, null));

        //when
        messageRepository.delete(id);

        //then
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    public void shouldSaveMultipleTimesFindAndDeleteMessage() {
        //given
        String messageContent = "hello world";
        Message message1 = generateJsonMessage(messageContent);
        Message message2 = generateJsonMessage(messageContent);
        final String id1 = message1.getId();
        final String id2 = message2.getId();
        String qualifiedName = "groupName.topic";

        Topic topic = topic(qualifiedName).build();

        //when
        messageRepository.save(message1, topic);
        messageRepository.save(message2, topic);
        messageRepository.save(message1, topic);
        messageRepository.save(message2, topic);

        //then
        assertThat(messageRepository.findAll()).contains(
            new BackupMessage(
                id1,
                messageContent.getBytes(),
                message1.getTimestamp(),
                qualifiedName,
                message1.getPartitionKey(),
                null,
                null
            )
        );

        //when
        messageRepository.delete(id1);

        //then
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).contains(
            new BackupMessage(
                id2,
                messageContent.getBytes(),
                message2.getTimestamp(),
                qualifiedName,
                message2.getPartitionKey(),
                null,
                null)
        );
    }

    @Test
    public void shouldCreateRepositoryFromFile() {
        //given
        String baseDir = Files.createTempDir().getAbsolutePath();
        File file = new File(baseDir, "messages.dat");

        //when
        new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

        //then
        assertThat(file).exists();
    }

    @Test
    public void shouldCreateRepositoryThenCloseAndRestore() {
        //given
        Message message = generateJsonMessage();
        String qualifiedName = "groupName.topic";
        Topic topic = topic(qualifiedName).build();

        String baseDir = Files.createTempDir().getAbsolutePath();
        File file = new File(baseDir, "messages.dat");

        messageRepository = new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

        //when
        messageRepository.save(message, topic);

        //then
        messageRepository.close();

        //when
        messageRepository = new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

        //then
        assertThat(messageRepository.findAll()).contains(new BackupMessage(
                message.getId(), message.getData(), message.getTimestamp(), qualifiedName, message.getPartitionKey(), null, null));
    }

    @Test
    public void shouldSaveFindAndDeleteMessageAvroMessage() {
        //given
        String qualifiedName = "groupName.topic";

        AvroUser avroUser = new AvroUser("Bob", 18, "blue");
        String id = MessageIdGenerator.generate();
        Message message = new AvroMessage(id, avroUser.asBytes(), System.currentTimeMillis(),
                CompiledSchema.of(AvroUserSchemaLoader.load(), 1, 1), "partition-key");

        byte[] messageContent = message.getData();
        long timestamp = message.getTimestamp();

        Topic topic = topic(qualifiedName).build();

        //when
        messageRepository.save(message, topic);

        //then
        assertThat(messageRepository.findAll()).contains(
                new BackupMessage(id, messageContent, timestamp, qualifiedName, message.getPartitionKey(), 1, 1));

        //when
        messageRepository.delete(id);

        //then
        assertThat(messageRepository.findAll()).isEmpty();
    }

    private Message generateJsonMessage() {
        return generateJsonMessage(UUID.randomUUID().toString());
    }

    private Message generateJsonMessage(String content) {
        return generateJsonMessage(content, System.currentTimeMillis());
    }

    private Message generateJsonMessage(String content, long timestamp) {
        byte[] messageContent = content.getBytes();
        String id = MessageIdGenerator.generate();
        return new JsonMessage(id, messageContent, timestamp, "partition-key");
    }
}
