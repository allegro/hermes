package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ConsumerAssignmentCache implements NodeCacheListener {

  private static final Logger logger = getLogger(ConsumerAssignmentCache.class);

  private final WorkloadRegistryPaths paths;
  private final String consumerId;
  private final NodeCache workloadNodeCache;
  private final ConsumerWorkloadDecoder consumerWorkloadDecoder;
  private final Set<SubscriptionName> currentlyAssignedSubscriptions =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Set<SubscriptionAssignmentAware> callbacks =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  public ConsumerAssignmentCache(
      CuratorFramework curator,
      String consumerId,
      String clusterName,
      ZookeeperPaths zookeeperPaths,
      SubscriptionIds subscriptionIds) {
    this.paths = new WorkloadRegistryPaths(zookeeperPaths, clusterName);
    this.consumerId = consumerId;

    String path = paths.consumerWorkloadPath(consumerId);
    this.workloadNodeCache = new NodeCache(curator, path);
    workloadNodeCache.getListenable().addListener(this);

    this.consumerWorkloadDecoder = new ConsumerWorkloadDecoder(subscriptionIds);
  }

  public void start() throws Exception {
    try {
      logger.info(
          "Starting binary workload assignment cache at {}, watching current consumer path at {}",
          paths.consumersWorkloadCurrentClusterRuntimeBinaryPath(),
          paths.consumerWorkloadPath(consumerId));
      workloadNodeCache.start(true);
    } catch (Exception e) {
      throw new IllegalStateException("Could not start node cache for consumer workload", e);
    }
    refreshConsumerWorkload();
  }

  private void refreshConsumerWorkload() {
    ChildData nodeData = workloadNodeCache.getCurrentData();
    if (nodeData != null) {
      byte[] data = nodeData.getData();
      Set<SubscriptionName> subscriptions = consumerWorkloadDecoder.decode(data);
      updateAssignedSubscriptions(subscriptions);
    } else {
      logger.info("No workload data available for consumer");
    }
  }

  private void updateAssignedSubscriptions(Set<SubscriptionName> targetAssignments) {
    ImmutableSet<SubscriptionName> assignmentDeletions =
        Sets.difference(currentlyAssignedSubscriptions, targetAssignments).immutableCopy();
    ImmutableSet<SubscriptionName> assignmentsAdditions =
        Sets.difference(targetAssignments, currentlyAssignedSubscriptions).immutableCopy();

    assignmentDeletions.forEach(
        s -> logger.info("Assignment deletion for subscription {}", s.getQualifiedName()));
    assignmentsAdditions.forEach(
        s -> logger.info("Assignment addition for subscription {}", s.getQualifiedName()));

    currentlyAssignedSubscriptions.clear();
    currentlyAssignedSubscriptions.addAll(targetAssignments);

    callbacks.forEach(
        callback -> {
          if (!callback.watchedConsumerId().isPresent()
              || callback.watchedConsumerId().get().equals(consumerId)) {
            assignmentDeletions.forEach(callback::onAssignmentRemoved);
            assignmentsAdditions.forEach(callback::onSubscriptionAssigned);
          }
        });
  }

  public void stop() throws Exception {
    try {
      logger.info("Stopping binary workload assignment cache");
      workloadNodeCache.close();
    } catch (IOException e) {
      throw new RuntimeException("Could not stop node cache for consumer workload", e);
    }
  }

  public boolean isAssignedTo(SubscriptionName subscription) {
    return currentlyAssignedSubscriptions.contains(subscription);
  }

  public void registerAssignmentCallback(SubscriptionAssignmentAware callback) {
    callbacks.add(callback);
  }

  public Set<SubscriptionName> getConsumerSubscriptions() {
    return ImmutableSet.copyOf(currentlyAssignedSubscriptions);
  }

  @Override
  public void nodeChanged() {
    refreshConsumerWorkload();
  }
}
