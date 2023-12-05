package pl.allegro.tech.hermes.management.domain.consistency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.InconsistentGroup;
import pl.allegro.tech.hermes.api.InconsistentMetadata;
import pl.allegro.tech.hermes.api.InconsistentSubscription;
import pl.allegro.tech.hermes.api.InconsistentTopic;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class DcConsistencyService {
    private final ExecutorService executor;
    private final List<DatacenterBoundRepositoryHolder<GroupRepository>> groupRepositories;
    private final List<DatacenterBoundRepositoryHolder<TopicRepository>> topicRepositories;
    private final List<DatacenterBoundRepositoryHolder<SubscriptionRepository>> subscriptionRepositories;
    private final ObjectMapper objectMapper;

    public DcConsistencyService(RepositoryManager repositoryManager,
                                ObjectMapper objectMapper,
                                ConsistencyCheckerProperties properties) {
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
    }

    @PreDestroy
    public void stop() {
        executor.shutdown();
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
