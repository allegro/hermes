package pl.allegro.tech.hermes.frontend.buffer;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

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
    // given
    String qualifiedName = "groupName.topic";

    Message message = generateJsonMessage();
    String id = message.getId();

    Topic topic = topic(qualifiedName).build();

    // when
    messageRepository.save(message, topic);

    // then
    assertThat(messageRepository.findAll()).contains(backupMessage(message, qualifiedName));

    // when
    messageRepository.delete(id);

    // then
    assertThat(messageRepository.findAll()).isEmpty();
  }

  @Test
  public void shouldSaveMultipleTimesFindAndDeleteMessage() {
    // given
    String messageContent = "hello world";
    Message message1 = generateJsonMessage(messageContent);
    Message message2 = generateJsonMessage(messageContent);
    String qualifiedName = "groupName.topic";

    Topic topic = topic(qualifiedName).build();

    // when
    messageRepository.save(message1, topic);
    messageRepository.save(message2, topic);
    messageRepository.save(message1, topic);
    messageRepository.save(message2, topic);

    // then
    assertThat(messageRepository.findAll()).contains(backupMessage(message1, qualifiedName));

    // when
    messageRepository.delete(message1.getId());

    // then
    assertThat(messageRepository.findAll()).hasSize(1);
    assertThat(messageRepository.findAll()).contains(backupMessage(message2, qualifiedName));
  }

  @Test
  public void shouldCreateRepositoryFromFile() throws IOException {
    // given
    File file = Files.createTempFile("backup", "messages.dat").toFile();

    // when
    new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

    // then
    assertThat(file).exists();
  }

  @Test
  public void shouldCreateRepositoryThenCloseAndRestore() throws IOException {
    // given
    Message message = generateJsonMessage();
    String qualifiedName = "groupName.topic";
    Topic topic = topic(qualifiedName).build();

    File file = Files.createTempFile("backup", "messages.dat").toFile();

    messageRepository = new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

    // when
    messageRepository.save(message, topic);

    // then
    messageRepository.close();

    // when
    messageRepository = new ChronicleMapMessageRepository(file, ENTRIES, AVERAGE_MESSAGE_SIZE);

    // then
    assertThat(messageRepository.findAll()).contains(backupMessage(message, qualifiedName));
  }

  @Test
  public void shouldSaveFindAndDeleteMessageAvroMessage() {
    // given
    String qualifiedName = "groupName.topic";

    AvroUser avroUser = new AvroUser("Bob", 18, "blue");
    String id = MessageIdGenerator.generate();
    Message message =
        new AvroMessage(
            id,
            avroUser.asBytes(),
            System.currentTimeMillis(),
            CompiledSchema.of(AvroUserSchemaLoader.load(), 1, 1),
            "partition-key",
            emptyMap());

    Topic topic = topic(qualifiedName).build();

    // when
    messageRepository.save(message, topic);

    // then
    assertThat(messageRepository.findAll()).contains(backupMessage(message, qualifiedName));

    // when
    messageRepository.delete(id);

    // then
    assertThat(messageRepository.findAll()).isEmpty();
  }

  @Test
  public void shouldLoadMessagesSavedByVersion2_5_0() throws IOException {
    // given
    // The hermes-buffer-v3_2-5-0.dat file has been generated by running
    // pl.allegro.tech.hermes.integration.MessageBufferLoadingTest#backupFileWithOneMessage
    // against version 2.5.0 (https://github.com/allegro/hermes/tree/hermes-2.5.0).
    InputStream fileSavedBy2_5_0 = getClass().getResourceAsStream("/hermes-buffer-v3_2-5-0.dat");
    String tempDirPath = Files.createTempDirectory("backup").toAbsolutePath().toString();
    Path backupFile = Path.of(tempDirPath, "/hermes-buffer-v3.dat");
    Files.copy(fileSavedBy2_5_0, backupFile);

    ChronicleMapMessageRepository messageRepository =
        new ChronicleMapMessageRepository(backupFile.toFile(), ENTRIES, AVERAGE_MESSAGE_SIZE);

    // when
    List<BackupMessage> loadedMessages = messageRepository.findAll();

    // then
    assertThat(loadedMessages).hasSize(1);
    BackupMessage message = loadedMessages.get(0);
    assertThat(message.getMessageId()).isEqualTo("573e570c-890d-49c6-8916-434a1ecb6c66");
    assertThat(message.getTimestamp()).isEqualTo(1704280749690L);
    assertThat(message.getQualifiedTopicName())
        .isEqualTo("backupGroup.topic-2e760871-954e-4823-935c-dfdadbb1be09");
    assertThat(message.getPartitionKey()).isNull();
    assertThat(message.getPropagatedHTTPHeaders()).isEmpty();
    assertThat(message.getSchemaVersion()).isNull();
    assertThat(message.getSchemaId()).isNull();

    JsonMessageContentWrapper contentWrapper =
        new JsonMessageContentWrapper("message", "metadata", new ObjectMapper());
    CompositeMessageContentWrapper wrapper =
        new CompositeMessageContentWrapper(contentWrapper, null, null, null, null, null);
    byte[] content =
        wrapper.wrapJson(
            "message".getBytes(defaultCharset()),
            message.getMessageId(),
            message.getTimestamp(),
            emptyMap());
    assertThat(message.getData()).isEqualTo(content);
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
    return new JsonMessage(
        id, messageContent, timestamp, "partition-key", Map.of("propagated-http-header", "value"));
  }

  private BackupMessage backupMessage(Message m, String qualifiedTopicName) {
    return new BackupMessage(
        m.getId(),
        m.getData(),
        m.getTimestamp(),
        qualifiedTopicName,
        m.getPartitionKey(),
        m.getCompiledSchema().map(cs -> cs.getVersion().value()).orElse(null),
        m.getCompiledSchema().map(cs -> cs.getId().value()).orElse(null),
        m.getHTTPHeaders());
  }
}
