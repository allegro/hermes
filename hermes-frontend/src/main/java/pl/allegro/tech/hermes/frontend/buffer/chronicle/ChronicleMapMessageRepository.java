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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ChronicleMapMessageRepository implements MessageRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChronicleMapMessageRepository.class);

    private static final boolean SAME_BUILDER_CONFIG = false;

    private final ChronicleMap<String, ChronicleMapEntryValue> map;

    private boolean closed = false;
    private final ReadWriteLock closeLock = new ReentrantReadWriteLock();

    static {
        System.setProperty("chronicle.map.disable.locking", Boolean.TRUE.toString());
    }

    public ChronicleMapMessageRepository(File file, int entries, int averageMessageSize) {
        logger.info("Creating backup storage in path: {}", file.getAbsolutePath());
        try {
            map = ChronicleMapBuilder.of(String.class, ChronicleMapEntryValue.class)
                    .constantKeySizeBySample(MessageIdGenerator.generate())
                    .averageValueSize(averageMessageSize)
                    .entries(entries)
                    .setPreShutdownAction(new LoggingMapSizePreShutdownHook())
                    .sparseFile(true)
                    .createOrRecoverPersistedTo(file, SAME_BUILDER_CONFIG);
        } catch (IOException e) {
            logger.error("Failed to load backup storage file from path {}", file.getAbsoluteFile(), e);
            throw new ChronicleMapCreationException(e);
        }
    }

    public ChronicleMapMessageRepository(File file, int entries, int averageMessageSize, HermesMetrics hermesMetrics) {
        this(file, entries, averageMessageSize);
        hermesMetrics.registerMessageRepositorySizeGauge(map::size);
    }

    @Override
    public void save(Message message, Topic topic) {
        Lock lock = closeLock.readLock();
        lock.lock();
        try {
            if (closed) {
                throw new ChronicleMapClosedException("Backup storage is closed. Unable to add new messages.");
            }
            map.put(message.getId(), new ChronicleMapEntryValue(message.getData(), message.getTimestamp(), topic.getQualifiedName(), message.getPartitionKey()));
        } finally {
            lock.unlock();
        }
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

    private class LoggingMapSizePreShutdownHook implements Runnable {

        @Override
        public void run() {
            Lock lock = closeLock.writeLock();
            lock.lock();
            try {
                closed = true;
                if (map != null) {
                    logger.info("Closing backup storage with {} messages.", map.size());
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
