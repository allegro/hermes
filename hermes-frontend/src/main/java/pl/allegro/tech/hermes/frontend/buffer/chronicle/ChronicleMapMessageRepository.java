package pl.allegro.tech.hermes.frontend.buffer.chronicle;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessage;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ChronicleMapMessageRepository implements MessageRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChronicleMapMessageRepository.class);

    private ChronicleMap<String, ChronicleMapEntryValue> map;

    public ChronicleMapMessageRepository(File file) {
        try {
            logger.info("Creating backup storage in path: {}", file.getAbsolutePath());
            map = ChronicleMapBuilder.of(String.class, ChronicleMapEntryValue.class).createPersistedTo(file);
        } catch (IOException e) {
            logger.error("Failed to load backup storage file from path {}", file.getAbsoluteFile(), e);
            throw new ChronicleMapCreationException(e);
        }

        if(map == null) {
            logger.error("Backup file could not be read - check if it was not corrupted.");
        }
    }

    @Override
    public void save(Message message, Topic topic) {
        map.put(message.getId(), new ChronicleMapEntryValue(message.getData(), message.getTimestamp(), topic.getQualifiedName()));
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
        return new BackupMessage(id, entryValue.getData(), entryValue.getTimestamp(), entryValue.getQualifiedTopicName());
    }
}
