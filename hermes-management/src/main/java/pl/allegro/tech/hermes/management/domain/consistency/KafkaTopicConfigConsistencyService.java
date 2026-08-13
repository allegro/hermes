package pl.allegro.tech.hermes.management.domain.consistency;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.InconsistentKafkaTopic;
import pl.allegro.tech.hermes.api.KafkaTopicConfigDiff;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.management.config.KafkaConsistencyProperties;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.OwnedTopicConfig;

public class KafkaTopicConfigConsistencyService {

  private static final Logger logger =
      LoggerFactory.getLogger(KafkaTopicConfigConsistencyService.class);
  private static final int CONFIG_READ_BATCH_SIZE = 250;

  private final TopicManagement topicManagement;
  private final MultiDCAwareService multiDCAwareService;
  private final TopicProperties topicProperties;
  private final TopicConfigDiffer differ;
  private final KafkaConsistencyProperties properties;
  private final MetricsFacade metricsFacade;
  private final Map<String, AtomicInteger> inconsistentTopicCounts = new ConcurrentHashMap<>();
  private final ExecutorService executor;
  private final ScheduledExecutorService scheduler;

  public KafkaTopicConfigConsistencyService(
      TopicManagement topicManagement,
      MultiDCAwareService multiDCAwareService,
      TopicProperties topicProperties,
      TopicConfigDiffer differ,
      KafkaConsistencyProperties properties,
      MetricsFacade metricsFacade) {
    this.topicManagement = topicManagement;
    this.multiDCAwareService = multiDCAwareService;
    this.topicProperties = topicProperties;
    this.differ = differ;
    this.properties = properties;
    this.metricsFacade = metricsFacade;
    this.executor =
        Executors.newFixedThreadPool(
            Math.max(1, properties.getThreadPoolSize()),
            new ThreadFactoryBuilder().setNameFormat("kafka-consistency-%d").build());
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("kafka-consistency-scheduler-%d").build());
    if (properties.isEnabled() && properties.isPeriodicCheckEnabled()) {
      registerInconsistencyGauges();
      scheduler.scheduleAtFixedRate(
          this::reportConsistency,
          properties.getInitialRefreshDelay().toSeconds(),
          properties.getRefreshInterval().toSeconds(),
          TimeUnit.SECONDS);
    }
  }

  @PreDestroy
  public void stop() {
    shutdownGracefully(scheduler);
    shutdownGracefully(executor);
  }

  private void shutdownGracefully(ExecutorService executorService) {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public List<String> listClusterNames() {
    return multiDCAwareService.getClusters().stream()
        .map(BrokersClusterService::getClusterName)
        .sorted()
        .toList();
  }

  public List<InconsistentKafkaTopic> listInconsistencies(Optional<String> clusterName) {
    List<Topic> topics = topicManagement.getAllTopics();
    List<ClusterInspection> inspections =
        selectClusters(clusterName).stream()
            .map(
                cluster ->
                    new ClusterInspection(
                        cluster.getClusterName(),
                        executor.submit(() -> inspectCluster(cluster, topics))))
            .toList();
    List<ClusterInspectionResult> results = inspections.stream().map(this::resolve).toList();
    List<ClusterInspectionResult> failures =
        results.stream().filter(result -> result.failure() != null).toList();
    if (!failures.isEmpty()) {
      String failedClusters =
          failures.stream()
              .map(ClusterInspectionResult::clusterName)
              .collect(Collectors.joining(", "));
      throw new ConsistencyCheckingException(
          "Fetching Kafka topic configs failed for clusters: " + failedClusters,
          failures.getFirst().failure());
    }
    return results.stream()
        .map(ClusterInspectionResult::inconsistencies)
        .flatMap(Collection::stream)
        .sorted(inconsistencyComparator())
        .toList();
  }

  public List<InconsistentKafkaTopic> syncConfigs(Optional<String> clusterName, boolean dryRun) {
    if (dryRun || clusterName.isPresent()) {
      List<InconsistentKafkaTopic> inconsistencies =
          listInconsistencies(clusterName).stream()
              .filter(InconsistentKafkaTopic::existsOnBroker)
              .toList();
      if (!dryRun) {
        applyConfigUpdates(inconsistencies);
      }
      return inconsistencies;
    }

    List<Topic> topics = topicManagement.getAllTopics();
    List<InconsistentKafkaTopic> inconsistencies = new ArrayList<>();
    List<Exception> failures = new ArrayList<>();
    for (BrokersClusterService cluster : multiDCAwareService.getClusters()) {
      try {
        List<InconsistentKafkaTopic> clusterInconsistencies =
            inspectCluster(cluster, topics).stream()
                .filter(InconsistentKafkaTopic::existsOnBroker)
                .toList();
        applyConfigUpdates(clusterInconsistencies);
        inconsistencies.addAll(clusterInconsistencies);
      } catch (Exception e) {
        failures.add(e);
        logger.warn("Syncing Kafka topic configs failed for {}", cluster.getClusterName(), e);
      }
    }
    if (!failures.isEmpty()) {
      throw new ConsistencyCheckingException(
          "Syncing Kafka topic configs failed for one or more clusters", failures.getFirst());
    }
    return inconsistencies.stream().sorted(inconsistencyComparator()).toList();
  }

  public List<String> bootstrapMissingTopics(String clusterName, boolean dryRun) {
    BrokersClusterService cluster = multiDCAwareService.getCluster(clusterName);
    validatePartitions(cluster);
    Set<String> existingTopicNames = cluster.listTopicNames();
    List<MissingTopic> missingTopics =
        topicManagement.getAllTopics().stream()
            .flatMap(
                topic ->
                    cluster.toKafkaTopics(topic).stream()
                        .filter(
                            kafkaTopic ->
                                !existingTopicNames.contains(kafkaTopic.name().asString()))
                        .map(kafkaTopic -> new MissingTopic(topic, kafkaTopic)))
            .toList();
    if (!dryRun) {
      applyInBatches(
          missingTopics,
          missing -> cluster.createTopic(missing.topic(), missing.kafkaTopic()),
          "Creating missing Kafka topics failed");
    }
    return missingTopics.stream().map(missing -> missing.kafkaTopic().name().asString()).toList();
  }

  public Optional<InconsistentKafkaTopic> inspectTopic(TopicName name, String clusterName) {
    Topic topic = topicManagement.getTopicDetails(name);
    List<InconsistentKafkaTopic> inconsistencies =
        inspectCluster(multiDCAwareService.getCluster(clusterName), List.of(topic));
    return inconsistencies.stream().findFirst();
  }

  public InconsistentKafkaTopic syncTopic(
      TopicName name, String kafkaTopicName, String clusterName, boolean dryRun) {
    Topic topic = topicManagement.getTopicDetails(name);
    List<InconsistentKafkaTopic> inconsistencies =
        inspectCluster(multiDCAwareService.getCluster(clusterName), List.of(topic));
    Optional<InconsistentKafkaTopic> selected =
        inconsistencies.stream()
            .filter(item -> item.kafkaTopicName().equals(kafkaTopicName))
            .findFirst();
    if (!dryRun && selected.isPresent()) {
      applyConfigUpdates(List.of(selected.get()));
    }
    return selected.orElse(null);
  }

  private List<InconsistentKafkaTopic> inspectCluster(
      BrokersClusterService cluster, List<Topic> topics) {
    Map<KafkaTopic, Topic> hermesTopicByKafkaTopic = new HashMap<>();
    topics.forEach(
        topic ->
            cluster.toKafkaTopics(topic).stream()
                .forEach(kafkaTopic -> hermesTopicByKafkaTopic.put(kafkaTopic, topic)));

    Set<String> existingTopicNames = cluster.listTopicNames();
    List<KafkaTopic> existingKafkaTopics =
        hermesTopicByKafkaTopic.keySet().stream()
            .filter(topic -> existingTopicNames.contains(topic.name().asString()))
            .toList();
    Map<KafkaTopic, Map<String, String>> actualConfigs =
        readConfigsInBatches(cluster, existingKafkaTopics);

    return hermesTopicByKafkaTopic.entrySet().stream()
        .map(
            entry ->
                inspectKafkaTopic(
                    cluster.getClusterName(),
                    entry.getValue(),
                    entry.getKey(),
                    existingTopicNames,
                    actualConfigs))
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<InconsistentKafkaTopic> inspectKafkaTopic(
      String clusterName,
      Topic topic,
      KafkaTopic kafkaTopic,
      Set<String> existingTopicNames,
      Map<KafkaTopic, Map<String, String>> actualConfigs) {
    String kafkaTopicName = kafkaTopic.name().asString();
    boolean exists = existingTopicNames.contains(kafkaTopicName);
    List<KafkaTopicConfigDiff> configDiffs = List.of();
    if (exists) {
      configDiffs =
          differ.diff(desiredConfig(topic), actualConfigs.getOrDefault(kafkaTopic, Map.of()));
      if (configDiffs.isEmpty()) {
        return Optional.empty();
      }
    }
    return Optional.of(
        new InconsistentKafkaTopic(
            topic.getQualifiedName(), kafkaTopicName, clusterName, exists, configDiffs));
  }

  private Map<KafkaTopic, Map<String, String>> readConfigsInBatches(
      BrokersClusterService cluster, List<KafkaTopic> kafkaTopics) {
    Map<KafkaTopic, Map<String, String>> result = new HashMap<>();
    for (int from = 0; from < kafkaTopics.size(); from += CONFIG_READ_BATCH_SIZE) {
      int to = Math.min(from + CONFIG_READ_BATCH_SIZE, kafkaTopics.size());
      result.putAll(cluster.readTopicConfigs(kafkaTopics.subList(from, to)));
    }
    return result;
  }

  private void applyConfigUpdates(List<InconsistentKafkaTopic> inconsistencies) {
    Map<String, Topic> topicsByName =
        topicManagement.getAllTopics().stream()
            .collect(Collectors.toMap(Topic::getQualifiedName, topic -> topic));
    List<InconsistentKafkaTopic> existingTopics =
        inconsistencies.stream().filter(InconsistentKafkaTopic::existsOnBroker).toList();
    applyInBatches(
        existingTopics,
        item -> applyConfigUpdate(item, topicsByName),
        "Updating Kafka topic configs failed");
  }

  private <T> void applyInBatches(List<T> items, Consumer<T> action, String failureMessage) {
    int batchSize = Math.max(1, properties.getSyncBatchSize());
    List<Exception> failures = new ArrayList<>();
    for (int from = 0; from < items.size(); from += batchSize) {
      int to = Math.min(from + batchSize, items.size());
      List<Future<?>> futures = new ArrayList<>();
      items
          .subList(from, to)
          .forEach(item -> futures.add(executor.submit(() -> action.accept(item))));
      futures.forEach(
          future -> {
            try {
              future.get();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              failures.add(e);
            } catch (Exception e) {
              failures.add(e);
            }
          });
    }
    if (!failures.isEmpty()) {
      ConsistencyCheckingException exception =
          new ConsistencyCheckingException(failureMessage, failures.getFirst());
      failures.stream().skip(1).forEach(exception::addSuppressed);
      throw exception;
    }
  }

  private void applyConfigUpdate(
      InconsistentKafkaTopic inconsistency, Map<String, Topic> topicsByName) {
    Topic topic = topicsByName.get(inconsistency.qualifiedTopicName());
    if (topic == null) {
      logger.warn(
          "Skipping config update for {} on {}: topic no longer present in Hermes metadata",
          inconsistency.kafkaTopicName(),
          inconsistency.clusterName());
      return;
    }
    BrokersClusterService cluster = multiDCAwareService.getCluster(inconsistency.clusterName());
    cluster.toKafkaTopics(topic).stream()
        .filter(kafkaTopic -> kafkaTopic.name().asString().equals(inconsistency.kafkaTopicName()))
        .findFirst()
        .ifPresent(kafkaTopic -> cluster.updateTopicConfig(kafkaTopic, desiredConfig(topic)));
  }

  private Map<String, String> desiredConfig(Topic topic) {
    return OwnedTopicConfig.desiredConfig(
        topic.getRetentionTime().getDurationInMillis(), topicProperties);
  }

  private List<BrokersClusterService> selectClusters(Optional<String> clusterName) {
    return clusterName
        .map(name -> List.of(multiDCAwareService.getCluster(name)))
        .orElseGet(multiDCAwareService::getClusters);
  }

  private void validatePartitions(BrokersClusterService cluster) {
    if (properties.isBootstrapStrictPartitions()
        && !topicProperties.getPartitionsPerDc().containsKey(cluster.getDatacenter())) {
      throw new IllegalStateException(
          "No partitionsPerDc mapping for datacenter " + cluster.getDatacenter());
    }
  }

  private Comparator<InconsistentKafkaTopic> inconsistencyComparator() {
    return Comparator.comparing(InconsistentKafkaTopic::clusterName)
        .thenComparing(InconsistentKafkaTopic::qualifiedTopicName)
        .thenComparing(InconsistentKafkaTopic::kafkaTopicName);
  }

  private void reportConsistency() {
    try {
      List<InconsistentKafkaTopic> inconsistencies = listInconsistencies(Optional.empty());
      updateInconsistencyGauges(inconsistencies);
      logger.info(
          "Kafka topic config consistency check found {} inconsistencies", inconsistencies.size());
    } catch (Exception e) {
      logger.warn("Kafka topic config consistency check failed", e);
    }
  }

  private void registerInconsistencyGauges() {
    multiDCAwareService.getClusters().stream()
        .map(BrokersClusterService::getClusterName)
        .forEach(this::registerInconsistencyGauge);
  }

  private void registerInconsistencyGauge(String clusterName) {
    inconsistentTopicCounts.computeIfAbsent(
        clusterName,
        name -> {
          AtomicInteger count = new AtomicInteger();
          metricsFacade
              .consistency()
              .registerKafkaTopicConfigInconsistenciesGauge(name, count, value -> value.get());
          return count;
        });
  }

  private void updateInconsistencyGauges(List<InconsistentKafkaTopic> inconsistencies) {
    Map<String, Long> countsByCluster =
        inconsistencies.stream()
            .collect(
                Collectors.groupingBy(InconsistentKafkaTopic::clusterName, Collectors.counting()));
    countsByCluster.keySet().forEach(this::registerInconsistencyGauge);
    inconsistentTopicCounts.forEach(
        (clusterName, count) ->
            count.set(countsByCluster.getOrDefault(clusterName, 0L).intValue()));
  }

  private ClusterInspectionResult resolve(ClusterInspection inspection) {
    try {
      return new ClusterInspectionResult(inspection.clusterName(), inspection.future().get(), null);
    } catch (Exception e) {
      return new ClusterInspectionResult(inspection.clusterName(), List.of(), e);
    }
  }

  private record ClusterInspection(
      String clusterName, Future<List<InconsistentKafkaTopic>> future) {}

  private record ClusterInspectionResult(
      String clusterName, List<InconsistentKafkaTopic> inconsistencies, Exception failure) {}

  private record MissingTopic(Topic topic, KafkaTopic kafkaTopic) {}
}
