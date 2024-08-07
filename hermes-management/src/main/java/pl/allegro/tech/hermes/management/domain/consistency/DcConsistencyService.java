package pl.allegro.tech.hermes.management.domain.consistency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.InconsistentGroup;
import pl.allegro.tech.hermes.api.InconsistentMetadata;
import pl.allegro.tech.hermes.api.InconsistentSubscription;
import pl.allegro.tech.hermes.api.InconsistentTopic;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.config.ConsistencyCheckerProperties;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class DcConsistencyService {
    private static final Logger logger = LoggerFactory.getLogger(DcConsistencyService.class);

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final List<DatacenterBoundRepositoryHolder<GroupRepository>> groupRepositories;
    private final List<DatacenterBoundRepositoryHolder<TopicRepository>> topicRepositories;
    private final List<DatacenterBoundRepositoryHolder<SubscriptionRepository>> subscriptionRepositories;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean isStorageConsistent = new AtomicBoolean(true);

    public DcConsistencyService(RepositoryManager repositoryManager,
                                ObjectMapper objectMapper,
                                ConsistencyCheckerProperties properties,
                                MetricsFacade metricsFacade) {
        this.groupRepositories = repositoryManager.getRepositories(GroupRepository.class);
        this.topicRepositories = repositoryManager.getRepositories(TopicRepository.class);
        this.subscriptionRepositories = repositoryManager.getRepositories(SubscriptionRepository.class);
        this.objectMapper = objectMapper;
        this.executor = Executors.newFixedThreadPool(
                properties.getThreadPoolSize(),
                new ThreadFactoryBuilder()
                        .setNameFormat("consistency-checker-%d")
                        .build()
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("consistency-checker-scheduler-%d")
                        .build()
        );
        if (properties.isPeriodicCheckEnabled()) {
            scheduler.scheduleAtFixedRate(this::reportConsistency,
                    properties.getInitialRefreshDelay().getSeconds(),
                    properties.getRefreshInterval().getSeconds(),
                    TimeUnit.SECONDS);
            metricsFacade.consistency().registerStorageConsistencyGauge(isStorageConsistent, isConsistent -> isConsistent.get() ? 1 : 0);
        }
    }

    @PreDestroy
    public void stop() {
        executor.shutdown();
        scheduler.shutdown();
    }

    private void reportConsistency() {
        long start = System.currentTimeMillis();
        Set<String> groups = listAllGroupNames();
        List<InconsistentGroup> inconsistentGroups = listInconsistentGroups(groups);
        long durationSeconds = (System.currentTimeMillis() - start) / 1000;
        logger.info("Consistency check finished in {}s, number of inconsistent groups: {}", durationSeconds, inconsistentGroups.size());
        isStorageConsistent.set(inconsistentGroups.isEmpty());
    }

    public List<InconsistentGroup> listInconsistentGroups(Set<String> groupNames) {
        List<InconsistentGroup> inconsistentGroups = new ArrayList<>();
        for (MetadataCopies copies : listCopiesOfGroups(groupNames)) {
            List<InconsistentMetadata> inconsistentMetadata = findInconsistentMetadata(copies);
            List<InconsistentTopic> inconsistentTopics = listInconsistentTopics(copies.getId());
            if (!inconsistentMetadata.isEmpty() || !inconsistentTopics.isEmpty()) {
                inconsistentGroups.add(new InconsistentGroup(copies.getId(), inconsistentMetadata, inconsistentTopics));
            }
        }
        return inconsistentGroups;
    }

    private record DatacenterRepositoryHolderSyncRequest<R>(
            DatacenterBoundRepositoryHolder<R> primaryHolder,
            List<DatacenterBoundRepositoryHolder<R>> replicaHolders
    ) {
    }

    private <R> DatacenterRepositoryHolderSyncRequest<R> partition(List<DatacenterBoundRepositoryHolder<R>> repositoryHolders, String primaryDatacenter) {
        List<DatacenterBoundRepositoryHolder<R>> replicas = new ArrayList<>();
        DatacenterBoundRepositoryHolder<R> primary = null;
        for (DatacenterBoundRepositoryHolder<R> repositoryHolder : repositoryHolders) {
            if (repositoryHolder.getDatacenterName().equals(primaryDatacenter)) {
                primary = repositoryHolder;
            } else {
                replicas.add(repositoryHolder);
            }
        }
        if (primary == null) {
            throw new SynchronizationException("Source of truth datacenter not found: " + primaryDatacenter);
        }
        return new DatacenterRepositoryHolderSyncRequest<>(primary, replicas);
    }

    public void syncGroup(String groupName, String sourceOfTruthDatacenter) {
        sync(groupRepositories, sourceOfTruthDatacenter,
                repo -> repo.groupExists(groupName),
                repo -> {
                    try {
                        return Optional.of(repo.getGroupDetails(groupName));
                    } catch (GroupNotExistsException ignored) {
                        return Optional.empty();
                    }
                },
                GroupRepository::createGroup,
                GroupRepository::updateGroup,
                repo -> repo.removeGroup(groupName)
        );
    }

    public void syncTopic(TopicName topicName, String sourceOfTruthDatacenter) {
        sync(topicRepositories, sourceOfTruthDatacenter,
                repo -> repo.topicExists(topicName),
                repo -> {
                    try {
                        return Optional.of(repo.getTopicDetails(topicName));
                    } catch (TopicNotExistsException ignored) {
                        return Optional.empty();
                    }
                },
                TopicRepository::createTopic,
                TopicRepository::updateTopic,
                repo -> repo.removeTopic(topicName)
        );
    }

    public void syncSubscription(SubscriptionName subscriptionName, String sourceOfTruthDatacenter) {
        sync(subscriptionRepositories, sourceOfTruthDatacenter,
                repo -> repo.subscriptionExists(subscriptionName.getTopicName(), subscriptionName.getName()),
                repo -> {
                    try {
                        return Optional.of(repo.getSubscriptionDetails(subscriptionName));
                    } catch (SubscriptionNotExistsException ignored) {
                        return Optional.empty();
                    }
                },
                SubscriptionRepository::createSubscription,
                SubscriptionRepository::updateSubscription,
                repo -> repo.removeSubscription(subscriptionName.getTopicName(), subscriptionName.getName())
        );
    }

    private <R, S> void sync(List<DatacenterBoundRepositoryHolder<R>> repositories,
                             String sourceOfTruthDatacenter,
                             Function<R, Boolean> exists,
                             Function<R, Optional<S>> get,
                             BiConsumer<R, S> create,
                             BiConsumer<R, S> update,
                             Consumer<R> delete
    ) {
        var request = partition(repositories, sourceOfTruthDatacenter);
        var primaryRepository = request.primaryHolder.getRepository();
        Optional<S> primary = get.apply(primaryRepository);
        var primaryPresent = primary.isPresent();

        for (var holder : request.replicaHolders) {
            var repository = holder.getRepository();
            var replicaPresent = exists.apply(repository);

            if (primaryPresent && replicaPresent) {
                update.accept(repository, primary.get());
            } else if (primaryPresent) {
                create.accept(repository, primary.get());
            } else if (replicaPresent) {
                delete.accept(repository);
            }
        }
    }

    private List<MetadataCopies> listCopiesOfGroups(Set<String> groupNames) {
        Map<String, Future<List<Group>>> futuresPerDatacenter = new HashMap<>();
        for (DatacenterBoundRepositoryHolder<GroupRepository> repositoryHolder : groupRepositories) {
            Future<List<Group>> future = executor.submit(() -> listGroups(repositoryHolder.getRepository(), groupNames));
            futuresPerDatacenter.put(repositoryHolder.getDatacenterName(), future);
        }
        return listCopies(futuresPerDatacenter, Group::getGroupName);
    }

    private List<Group> listGroups(GroupRepository repository, Set<String> groupNames) {
        List<Group> groups = new ArrayList<>();
        for (String groupName : groupNames) {
            try {
                Group group = repository.getGroupDetails(groupName);
                groups.add(group);
            } catch (GroupNotExistsException e) {
                // ignore
            }
        }
        return groups;
    }

    private List<InconsistentTopic> listInconsistentTopics(String group) {
        List<InconsistentTopic> inconsistentTopics = new ArrayList<>();
        for (MetadataCopies copies : listCopiesOfTopicsFromGroup(group)) {
            List<InconsistentMetadata> inconsistentMetadata = findInconsistentMetadata(copies);
            List<InconsistentSubscription> inconsistentSubscriptions = listInconsistentSubscriptions(copies.getId());
            if (!inconsistentMetadata.isEmpty() || !inconsistentSubscriptions.isEmpty()) {
                inconsistentTopics.add(new InconsistentTopic(copies.getId(), inconsistentMetadata, inconsistentSubscriptions));
            }
        }
        return inconsistentTopics;
    }

    private List<MetadataCopies> listCopiesOfTopicsFromGroup(String group) {
        Map<String, Future<List<Topic>>> futuresPerDatacenter = new HashMap<>();
        for (DatacenterBoundRepositoryHolder<TopicRepository> repositoryHolder : topicRepositories) {
            Future<List<Topic>> future = executor.submit(() -> listTopics(repositoryHolder.getRepository(), group));
            futuresPerDatacenter.put(repositoryHolder.getDatacenterName(), future);
        }
        return listCopies(futuresPerDatacenter, Topic::getQualifiedName);
    }

    private List<Topic> listTopics(TopicRepository topicRepository, String group) {
        try {
            return topicRepository.listTopics(group);
        } catch (GroupNotExistsException e) {
            return emptyList();
        }
    }

    private List<InconsistentSubscription> listInconsistentSubscriptions(String topic) {
        return listCopiesOfSubscriptionsFromTopic(topic).stream()
                .filter(copies -> !copies.areAllEqual())
                .map(copies -> new InconsistentSubscription(copies.getId(), findInconsistentMetadata(copies)))
                .collect(toList());
    }

    private List<MetadataCopies> listCopiesOfSubscriptionsFromTopic(String topic) {
        Map<String, Future<List<Subscription>>> futuresPerDatacenter = new HashMap<>();
        for (DatacenterBoundRepositoryHolder<SubscriptionRepository> repositoryHolder : subscriptionRepositories) {
            Future<List<Subscription>> future = executor.submit(
                    () -> listSubscriptions(repositoryHolder.getRepository(), topic)
            );
            futuresPerDatacenter.put(repositoryHolder.getDatacenterName(), future);
        }
        return listCopies(futuresPerDatacenter, subscription -> subscription.getQualifiedName().getQualifiedName());
    }

    private List<Subscription> listSubscriptions(SubscriptionRepository subscriptionRepository, String topic) {
        try {
            return subscriptionRepository.listSubscriptions(TopicName.fromQualifiedName(topic));
        } catch (TopicNotExistsException e) {
            return emptyList();
        }
    }

    private <T> List<MetadataCopies> listCopies(Map<String, Future<List<T>>> futuresPerDatacenter, Function<T, String> idResolver) {
        Map<String, MetadataCopies> copiesPerId = new HashMap<>();
        Set<String> datacenters = futuresPerDatacenter.keySet();
        for (Map.Entry<String, Future<List<T>>> entry : futuresPerDatacenter.entrySet()) {
            List<T> entities = resolveFuture(entry.getValue());
            String datacenter = entry.getKey();
            for (T entity : entities) {
                String id = idResolver.apply(entity);
                MetadataCopies copies = copiesPerId.getOrDefault(id, new MetadataCopies(id, datacenters));
                copies.put(datacenter, entity);
                copiesPerId.put(id, copies);
            }
        }
        return new ArrayList<>(copiesPerId.values());
    }

    private List<InconsistentMetadata> findInconsistentMetadata(MetadataCopies copies) {
        if (copies.areAllEqual()) {
            return emptyList();
        }
        return copies.getCopyPerDatacenter().entrySet().stream()
                .map(entry -> mapToInconsistentMetadata(entry.getKey(), entry.getValue()))
                .collect(toList());
    }

    private InconsistentMetadata mapToInconsistentMetadata(String id, Object content) {
        try {
            if (content == null) {
                return new InconsistentMetadata(id, null);
            }
            return new InconsistentMetadata(id, objectMapper.writeValueAsString(content));
        } catch (JsonProcessingException e) {
            throw new ConsistencyCheckingException("Metadata serialization failed", e);
        }
    }

    public Set<String> listAllGroupNames() {
        List<Future<List<String>>> results = new ArrayList<>();
        for (DatacenterBoundRepositoryHolder<GroupRepository> repositoryHolder : groupRepositories) {
            Future<List<String>> submit = executor.submit(() -> repositoryHolder.getRepository().listGroupNames());
            results.add(submit);
        }
        return results.stream().map(this::resolveFuture)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private <T> T resolveFuture(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new ConsistencyCheckingException("Fetching metadata failed", e);
        }
    }
}
