package pl.allegro.tech.hermes.frontend.buffer.chronicle;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessage;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;

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
      map =
          ChronicleMapBuilder.of(String.class, ChronicleMapEntryValue.class)
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

  public ChronicleMapMessageRepository(
      File file, int entries, int averageMessageSize, MetricsFacade metricsFacade) {
    this(file, entries, averageMessageSize);
    metricsFacade.persistentBuffer().registerBackupStorageSizeGauge(map, Map::size);
  }

  @Override
  public void save(Message message, Topic topic) {
    Lock lock = closeLock.readLock();
    lock.lock();
    try {
      if (closed) {
        throw new ChronicleMapClosedException(
            "Backup storage is closed. Unable to add new messages.");
      }
      map.put(
          message.getId(),
          new ChronicleMapEntryValue(
              message.getData(),
              message.getTimestamp(),
              topic.getQualifiedName(),
              message.getPartitionKey(),
              message.getCompiledSchema().map(v -> v.getVersion().value()).orElse(null),
              message.getCompiledSchema().map(v -> v.getId().value()).orElse(null),
              message.getHTTPHeaders()));
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
    return map.entrySet().stream()
        .map((e) -> toBackupMessage(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public void close() {
    map.close();
  }

  private BackupMessage toBackupMessage(String id, ChronicleMapEntryValue entryValue) {
    return new BackupMessage(
        id,
        entryValue.getData(),
        entryValue.getTimestamp(),
        entryValue.getQualifiedTopicName(),
        entryValue.getPartitionKey(),
        entryValue.getSchemaVersion(),
        entryValue.getSchemaId(),
        entryValue.getPropagatedHttpHeaders());
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
