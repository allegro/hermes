package pl.allegro.tech.hermes.frontend.buffer.chronicle;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessage;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ChronicleMapMessageRepository implements MessageRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChronicleMapMessageRepository.class);

    private static final boolean SAME_BUILDER_CONFIG = false;

    private ChronicleMap<String, ChronicleMapEntryValue> map;

    public ChronicleMapMessageRepository(File file, int entries, int averageMessageSize) {
        logger.info("Creating backup storage in path: {}", file.getAbsolutePath());
        try {
            map = ChronicleMapBuilder.of(String.class, ChronicleMapEntryValue.class)
                    .constantKeySizeBySample(MessageIdGenerator.generate())
                    .averageValueSize(averageMessageSize)
                    .entries(entries)
                    .createOrRecoverPersistedTo(file, SAME_BUILDER_CONFIG);
        } catch (IOException e) {
            logger.error("Failed to load backup storage file from path {}", file.getAbsoluteFile(), e);
            throw new ChronicleMapCreationException(e);
        }
    }

    public ChronicleMapMessageRepository(File file, int entries, int averageMessageSize, HermesMetrics hermesMetrics) {
        this(file, entries, averageMessageSize);
        hermesMetrics.registerMessageRepositorySizeGauge(() -> map.size());
    }

    @Override
    public void save(Message message, Topic topic) {
        map.put(message.getId(), new ChronicleMapEntryValue(message.getData(), message.getTimestamp(), topic.getQualifiedName(), message.getPartitionKey()));
    }

    @Override
    public void delete(String messageId) {
        map.remove(messageId);
    }

    @Override
    public List<BackupMessage> findAll() {
        return map.entrySet().stream().map((e) -> toBackupMessage(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Override
    public void close() {
        map.close();
    }

    private BackupMessage toBackupMessage(String id, ChronicleMapEntryValue entryValue) {
        return new BackupMessage(id, entryValue.getData(), entryValue.getTimestamp(), entryValue.getQualifiedTopicName(), entryValue.getPartitionKey());
    }
}
