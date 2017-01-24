package pl.allegro.tech.hermes.management.domain.topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TopicService {

    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

    private final boolean allowRemoval;
    private final TopicRepository topicRepository;
    private final GroupService groupService;

    private final TopicMetricsRepository metricRepository;
    private final MessagePreviewRepository messagePreviewRepository;
    private final MultiDCAwareService multiDCAwareService;
    private final TopicValidator topicValidator;
    private final TopicContentTypeMigrationService topicContentTypeMigrationService;
    private final Clock clock;
    private final Auditor auditor;

    @Autowired
    public TopicService(MultiDCAwareService multiDCAwareService,
                        TopicRepository topicRepository,
                        GroupService groupService,
                        TopicProperties topicProperties,
                        TopicMetricsRepository metricRepository,
                        TopicValidator topicValidator,
                        TopicContentTypeMigrationService topicContentTypeMigrationService,
                        MessagePreviewRepository messagePreviewRepository,
                        Clock clock,
                        Auditor auditor) {
        this.multiDCAwareService = multiDCAwareService;
        this.allowRemoval = topicProperties.isAllowRemoval();
        this.topicRepository = topicRepository;
        this.groupService = groupService;
        this.metricRepository = metricRepository;
        this.topicValidator = topicValidator;
        this.topicContentTypeMigrationService = topicContentTypeMigrationService;
        this.messagePreviewRepository = messagePreviewRepository;
        this.clock = clock;
        this.auditor = auditor;
    }

    public void createTopic(Topic topic, String createdBy) {
        topicValidator.ensureCreatedTopicIsValid(topic);
        topicRepository.createTopic(topic);

        if (!multiDCAwareService.topicExists(topic)) {
            createTopicInBrokers(topic);
            auditor.objectCreated(createdBy, topic);
        } else {
            logger.info("Skipping creation of topic {} on brokers, topic already exists", topic.getQualifiedName());
        }
    }

    private void createTopicInBrokers(Topic topic) {
        try {
            multiDCAwareService.manageTopic(brokerTopicManagement ->
                    brokerTopicManagement.createTopic(topic)
            );
        } catch (Exception exception) {
            logger.error(
                    String.format("Could not create topic %s, rollback topic creation.", topic.getQualifiedName()),
                    exception
            );
            topicRepository.removeTopic(topic.getName());
        }
    }

    public void removeTopic(Topic topic, String removedBy) {
        if (!allowRemoval) {
            throw new TopicRemovalDisabledException(topic);
        }
        topicRepository.removeTopic(topic.getName());
        multiDCAwareService.manageTopic(brokerTopicManagement -> brokerTopicManagement.removeTopic(topic));
        auditor.objectRemoved(removedBy, Topic.class.getSimpleName(), topic.getQualifiedName());
    }

    public void updateTopic(TopicName topicName, PatchData patch, String modifiedBy) {
        groupService.checkGroupExists(topicName.getGroupName());

        Topic retrieved = getTopicDetails(topicName);
        Topic modified = Patch.apply(retrieved, patch);

        topicValidator.ensureUpdatedTopicIsValid(modified, retrieved);

        if (!retrieved.equals(modified)) {
            Instant beforeMigrationInstant = clock.instant();
            if (retrieved.getRetentionTime() != modified.getRetentionTime()) {
                multiDCAwareService.manageTopic(brokerTopicManagement ->
                        brokerTopicManagement.updateTopic(modified)
                );
            }
            topicRepository.updateTopic(modified);
            if (!retrieved.wasMigratedFromJsonType() && modified.wasMigratedFromJsonType()) {
                topicContentTypeMigrationService.notifySubscriptions(modified, beforeMigrationInstant);
            }
            auditor.objectUpdated(modifiedBy, retrieved, modified);
        }
    }

    public void touchTopic(TopicName topicName) {
        topicRepository.touchTopic(topicName);
    }

    public List<String> listQualifiedTopicNames(String groupName) {
        return topicRepository.listTopicNames(groupName).stream()
                .map(topicName -> new TopicName(groupName, topicName).qualifiedName())
                .collect(Collectors.toList());
    }

    public List<Topic> listTopics(String groupName) {
        return topicRepository.listTopics(groupName);
    }

    public List<String> listQualifiedTopicNames() {
        return groupService.listGroupNames().stream()
                .map(this::listQualifiedTopicNames)
                .flatMap(List::stream)
                .sorted()
                .collect(Collectors.toList());
    }

    public Topic getTopicDetails(TopicName topicName) {
        return topicRepository.getTopicDetails(topicName);
    }

    public TopicMetrics getTopicMetrics(TopicName topicName) {
        return topicRepository.topicExists(topicName) ? metricRepository.loadMetrics(topicName) : TopicMetrics.unavailable();
    }

    public String fetchSingleMessageFromPrimary(String brokersClusterName, TopicName topicName, Integer partition, Long offset) {
        return multiDCAwareService.readMessageFromPrimary(brokersClusterName, getTopicDetails(topicName), partition, offset);
    }

    public List<String> listTrackedTopicNames() {
        return groupService.listGroupNames().stream()
                .map(topicRepository::listTopics)
                .flatMap(List::stream)
                .filter(Topic::isTrackingEnabled)
                .map(Topic::getQualifiedName)
                .collect(Collectors.toList());
    }

    public List<String> listTrackedTopicNames(String groupName) {
        return listTopics(groupName).stream()
                .filter(Topic::isTrackingEnabled)
                .map(Topic::getQualifiedName)
                .collect(Collectors.toList());
    }

    public List<String> listFilteredTopicNames(Query<Topic> query) {
        return queryTopic(query)
                .stream()
                .map(Topic::getQualifiedName)
                .collect(Collectors.toList());
    }

    public List<String> listFilteredTopicNames(String groupName, Query<Topic> query) {
        return query.filter(listTopics(groupName))
                .map(Topic::getQualifiedName)
                .collect(Collectors.toList());
    }

    public List<Topic> queryTopic(Query<Topic> query) {
        return query
                .filter(getAllTopics())
                .collect(Collectors.toList());
    }

    public List<Topic> getAllTopics() {
        return groupService
                .listGroupNames()
                .stream()
                .map(topicRepository::listTopics)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public Optional<byte[]> preview(TopicName topicName, int idx) {
        List<byte[]> result = messagePreviewRepository.loadPreview(topicName);
        if (idx >= 0 && idx < result.size()) {
            return Optional.of(result.get(idx));
        } else return Optional.empty();
    }

    public List<String> previewText(TopicName topicName) {
        List<byte[]> result = messagePreviewRepository.loadPreview(topicName);
        List<String> response = new ArrayList<>(result.size());
        for (byte[] r : result) {
            response.add(new String(r, StandardCharsets.UTF_8));
        }
        return response;
    }
}
