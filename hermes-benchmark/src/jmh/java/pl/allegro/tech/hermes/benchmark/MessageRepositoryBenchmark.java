package pl.allegro.tech.hermes.benchmark;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessage;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapCreationException;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapEntryValue;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Threads(value = 100)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MessageRepositoryBenchmark {

    @State(Scope.Benchmark)
    public static class Repositories {
        private static final int ENTRIES = 100;
        private static final int AVERAGE_MESSAGE_SIZE = 600;

        MessageRepository hermesImplMessageRepository;
        MessageRepository baselineMessageRepository;

        Message message;
        Topic topic;

        @Setup
        public void setup() throws IOException {
            message = generateMessage();
            topic = topic("groupName.topic").build();

            hermesImplMessageRepository = new ChronicleMapMessageRepository(prepareFile(), ENTRIES, AVERAGE_MESSAGE_SIZE);
            baselineMessageRepository = new BaselineChronicleMapMessageRepository(prepareFile(), ENTRIES, AVERAGE_MESSAGE_SIZE);
        }

        private Message generateMessage() {
            byte[] messageContent = UUID.randomUUID().toString().getBytes();
            String id = MessageIdGenerator.generate();
            return new JsonMessage(id, messageContent, System.currentTimeMillis(), "partition-key", emptyMap());
        }

        private File prepareFile() throws IOException {

            String baseDir = Files.createTempDirectory(null).toFile().getAbsolutePath();
            return new File(baseDir, "messages.dat");
        }
    }

    @Benchmark
    public void hermesImplSave(Repositories repositories) {
        repositories.hermesImplMessageRepository.save(repositories.message, repositories.topic);
    }

    @Benchmark
    public void baselineSave(Repositories repositories) {
        repositories.baselineMessageRepository.save(repositories.message, repositories.topic);
    }

    public static class BaselineChronicleMapMessageRepository implements MessageRepository {
        private static final boolean SAME_BUILDER_CONFIG = false;

        private final ChronicleMap<String, ChronicleMapEntryValue> map;

        public BaselineChronicleMapMessageRepository(File file, int entries, int averageMessageSize) {
            try {
                map = ChronicleMapBuilder.of(String.class, ChronicleMapEntryValue.class)
                        .constantKeySizeBySample(MessageIdGenerator.generate())
                        .averageValueSize(averageMessageSize)
                        .entries(entries)
                        .sparseFile(true)
                        .createOrRecoverPersistedTo(file, SAME_BUILDER_CONFIG);
            } catch (IOException e) {
                throw new ChronicleMapCreationException(e);
            }
        }

        @Override
        public void save(Message message, Topic topic) {
            ChronicleMapEntryValue entryValue = new ChronicleMapEntryValue(
                    message.getData(),
                    message.getTimestamp(),
                    topic.getQualifiedName(),
                    message.getPartitionKey(),
                    null,
                    null,
                    emptyMap());
            map.put(message.getId(), entryValue);
        }

        @Override
        public void delete(String messageId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<BackupMessage> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }
}
