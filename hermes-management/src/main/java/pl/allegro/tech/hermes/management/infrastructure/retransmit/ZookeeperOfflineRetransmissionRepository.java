package pl.allegro.tech.hermes.management.infrastructure.retransmit;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionValidationException;

public class ZookeeperOfflineRetransmissionRepository extends ZookeeperBasedRepository
    implements OfflineRetransmissionRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperOfflineRetransmissionRepository.class);

  public ZookeeperOfflineRetransmissionRepository(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    super(zookeeper, mapper, paths);
  }

  @Override
  public void saveTask(OfflineRetransmissionTask task) {
    logger.info("Saving retransmission task {}", task);
    try {
      createRecursively(paths.offlineRetransmissionPath(task.getTaskId()), task);
      logger.info("Successfully saved retransmission task {}", task);
    } catch (Exception ex) {
      String msg = format("Error while saving retransmission task %s", task.toString());
      throw new InternalProcessingException(msg, ex);
    }
  }

  @Override
  public List<OfflineRetransmissionTask> getAllTasks() {
    try {
      if (pathExists(paths.offlineRetransmissionPath())) {
        return childrenOf(paths.offlineRetransmissionPath()).stream()
            .map(
                id ->
                    readFrom(paths.offlineRetransmissionPath(id), OfflineRetransmissionTask.class))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception ex) {
      String msg = "Error while fetching offline retransmission tasks";
      throw new InternalProcessingException(msg, ex);
    }
  }

  @Override
  public void deleteTask(String taskId) {
    logger.info("Trying to delete retransmission task with id={}", taskId);
    try {
      ensureTaskExists(taskId);
      remove(paths.offlineRetransmissionPath(taskId));
      logger.info("Successfully deleted retransmission task with id={}", taskId);
    } catch (OfflineRetransmissionValidationException ex) {
      throw ex;
    } catch (Exception ex) {
      String msg = format("Error while deleting retransmission task with id=%s", taskId);
      throw new InternalProcessingException(msg, ex);
    }
  }

  private void ensureTaskExists(String taskId) {
    if (!pathExists(paths.offlineRetransmissionPath(taskId))) {
      String msg = String.format("Retransmission task with id %s does not exist.", taskId);
      throw new OfflineRetransmissionValidationException(msg);
    }
  }
}
