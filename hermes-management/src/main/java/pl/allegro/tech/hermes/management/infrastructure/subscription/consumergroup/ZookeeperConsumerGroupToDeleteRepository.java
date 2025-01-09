package pl.allegro.tech.hermes.management.infrastructure.subscription.consumergroup;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupAlreadyScheduledToDeleteException;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDelete;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteNotFoundException;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteRepository;

public class ZookeeperConsumerGroupToDeleteRepository extends ZookeeperBasedRepository
    implements ConsumerGroupToDeleteRepository {
  public ZookeeperConsumerGroupToDeleteRepository(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    super(zookeeper, mapper, paths);
  }

  @Override
  public void scheduleConsumerGroupToDeleteTask(ConsumerGroupToDelete consumerGroupToDelete) {
    String consumerGroupToDeletePath =
        paths.consumerGroupToDeletePath(
            consumerGroupToDelete.subscriptionName().getQualifiedName());

    try {
      createRecursively(consumerGroupToDeletePath, consumerGroupToDelete);
    } catch (KeeperException.NodeExistsException e) {
      throw new ConsumerGroupAlreadyScheduledToDeleteException(consumerGroupToDelete, e);
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }

  @Override
  public void deleteConsumerGroupToDeleteTask(ConsumerGroupToDelete consumerGroupToDelete) {
    String consumerGroupToDeletePath =
        paths.consumerGroupToDeletePath(
            consumerGroupToDelete.subscriptionName().getQualifiedName());

    ensureConsumerGroupToDeleteExists(consumerGroupToDeletePath);

    try {
      remove(consumerGroupToDeletePath);
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }

  private void ensureConsumerGroupToDeleteExists(String consumerGroupToDeletePath) {
    if (!pathExists(consumerGroupToDeletePath)) {
      throw new ConsumerGroupToDeleteNotFoundException(consumerGroupToDeletePath);
    }
  }

  @Override
  public List<ConsumerGroupToDelete> getAllConsumerGroupsToDelete() {
    try {
      if (pathExists(paths.consumerGroupToDeletePath())) {
        return childrenOf(paths.consumerGroupToDeletePath()).stream()
            .map(id -> readFrom(paths.consumerGroupToDeletePath(id), ConsumerGroupToDelete.class))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }
}
