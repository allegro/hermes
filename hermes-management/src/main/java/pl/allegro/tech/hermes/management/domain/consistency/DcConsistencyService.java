package pl.allegro.tech.hermes.management.domain.consistency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.InconsistentGroup;
import pl.allegro.tech.hermes.api.InconsistentMetadata;
import pl.allegro.tech.hermes.api.InconsistentSubscription;
import pl.allegro.tech.hermes.api.InconsistentTopic;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZKTreeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class DcConsistencyService {
    private final List<DatacenterBoundRepositoryHolder<ZKTreeCache>> checkers;
    private final ObjectMapper objectMapper;

    public DcConsistencyService(List<DatacenterBoundRepositoryHolder<ZKTreeCache>> checkers,
                                ObjectMapper objectMapper) {
        this.checkers = checkers;
        this.objectMapper = objectMapper;
    }

    public List<InconsistentGroup> listInconsistentGroups() {
        List<InconsistentGroup> inconsistentGroups = new ArrayList<>();
        for (MetadataCopies copies : listCopiesOfGroups()) {
            List<InconsistentMetadata> inconsistentMetadata = findInconsistentMetadata(copies);
            List<InconsistentTopic> inconsistentTopics = listInconsistentTopics(copies.getId());
            if (!inconsistentMetadata.isEmpty() || !inconsistentTopics.isEmpty()) {
                inconsistentGroups.add(new InconsistentGroup(copies.getId(), inconsistentMetadata, inconsistentTopics));
            }
        }
        return inconsistentGroups;
    }

    private List<MetadataCopies> listCopiesOfGroups() {
        Map<String, List<Group>> groupsPerDatacenter = new HashMap<>();
        for (DatacenterBoundRepositoryHolder<ZKTreeCache> repositoryHolder : checkers) {
            List<Group> groups = listGroups(repositoryHolder.getRepository());
            groupsPerDatacenter.put(repositoryHolder.getDatacenterName(), groups);
        }
        return listCopies(groupsPerDatacenter, Group::getGroupName);
    }

    private List<Group> listGroups(ZKTreeCache repository) {
        return repository.getGroups();
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
        Map<String, List<Topic>> topicPerDatacenter = new HashMap<>();
        for (DatacenterBoundRepositoryHolder<ZKTreeCache> repositoryHolder : checkers) {
            List<Topic> topics = listTopics(repositoryHolder.getRepository(), group);
            topicPerDatacenter.put(repositoryHolder.getDatacenterName(), topics);
        }
        return listCopies(topicPerDatacenter, Topic::getQualifiedName);
    }

    private List<Topic> listTopics(ZKTreeCache topicRepository, String group) {
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
        Map<String, List<Subscription>> subscriptionsPerDatacenter = new HashMap<>();
        for (DatacenterBoundRepositoryHolder<ZKTreeCache> repositoryHolder : checkers) {
            List<Subscription> subscriptions = listSubscriptions(repositoryHolder.getRepository(), topic);
            subscriptionsPerDatacenter.put(repositoryHolder.getDatacenterName(), subscriptions);
        }
        return listCopies(subscriptionsPerDatacenter, subscription -> subscription.getQualifiedName().getQualifiedName());
    }

    private List<Subscription> listSubscriptions(ZKTreeCache checker, String topic) {
        try {
            return checker.listSubscriptions(TopicName.fromQualifiedName(topic));
        } catch (TopicNotExistsException e) {
            return emptyList();
        }
    }

    private <T> List<MetadataCopies> listCopies(Map<String, List<T>> entitiesPerDatacenter, Function<T, String> idResolver) {
        Map<String, MetadataCopies> copiesPerId = new HashMap<>();
        Set<String> datacenters = entitiesPerDatacenter.keySet();
        for (Map.Entry<String, List<T>> entry : entitiesPerDatacenter.entrySet()) {
            List<T> entities = entry.getValue();
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
        List<List<String>> results = new ArrayList<>();
        for (DatacenterBoundRepositoryHolder<ZKTreeCache> repositoryHolder : checkers) {
            List<String> groups = repositoryHolder.getRepository().listGroupNames();
            results.add(groups);
        }
        return results.stream()
                .flatMap(Collection::stream)
                .collect(toSet());
    }
}
