package pl.allegro.tech.hermes.management.domain.topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
public class TopicService {

    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

    private final boolean allowRemoval;
    private final TopicRepository topicRepository;
    private final GroupService groupService;

    private final TopicMetricsRepository metricRepository;
    private final SchemaValidator schemaValidator;
    private final MultiDCAwareService multiDCAwareService;

    @Inject
    public TopicService(MultiDCAwareService multiDCAwareService,
                        TopicRepository topicRepository,
                        GroupService groupService,
                        TopicProperties topicProperties,
                        TopicMetricsRepository metricRepository,
                        SchemaValidator schemaValidator) {
        this.multiDCAwareService = multiDCAwareService;
        this.allowRemoval = topicProperties.isAllowRemoval();
        this.topicRepository = topicRepository;
        this.groupService = groupService;
        this.metricRepository = metricRepository;
        this.schemaValidator = schemaValidator;
    }

    public void createTopic(Topic topic) {
        if (!isNullOrEmpty(topic.getMessageSchema())) {
            schemaValidator.check(topic.getMessageSchema());
        }
        topicRepository.createTopic(topic);

        try {
            multiDCAwareService.manageTopic(brokerTopicManagement ->
                brokerTopicManagement.createTopic(topic.getName(), topic.getRetentionTime())
            );
        } catch (Exception exception) {
            logger.error(
                    String.format("Could not create topic %s, rollback topic creation.", topic.getQualifiedName()),
                    exception
            );
            topicRepository.removeTopic(topic.getName());
        }
    }

    public void removeTopic(TopicName topicName) {
        if (!allowRemoval) {
            throw new TopicRemovalDisabledException(topicName);
        }
        topicRepository.removeTopic(topicName);
        multiDCAwareService.manageTopic(brokerTopicManagement -> brokerTopicManagement.removeTopic(topicName));
    }

    public void updateTopic(Topic topic) {
        groupService.checkGroupExists(topic.getName().getGroupName());

        Topic retrieved = getTopicDetails(topic.getName());
        Topic modified = Patch.apply(retrieved, topic);

        if (!retrieved.equals(modified)) {
            if (!isNullOrEmpty(modified.getMessageSchema())) {
                schemaValidator.check(modified.getMessageSchema());
            }

            if (retrieved.getRetentionTime() != modified.getRetentionTime()) {
                multiDCAwareService.manageTopic(brokerTopicManagement ->
                    brokerTopicManagement.updateTopic(topic.getName(), modified.getRetentionTime())
                );
            }
            topicRepository.updateTopic(modified);
        }
    }

    public List<String> listQualifiedTopicNames(String groupName) {
        return topicRepository.listTopicNames(groupName).stream()
                .map(topicName -> new TopicName(groupName, topicName).qualifiedName())
                .collect(Collectors.toList());
    }

    public List<String> listQualifiedTopicNames() {
        return groupService.listGroups().stream()
                .map(this::listQualifiedTopicNames)
                .flatMap(List::stream)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Topic> listTopics(String groupName) {
        return topicRepository.listTopics(groupName);
    }

    public Topic getTopicDetails(TopicName topicName) {
        return topicRepository.getTopicDetails(topicName);
    }

    public TopicMetrics getTopicMetrics(TopicName topicName) {
        return topicRepository.topicExists(topicName) ? metricRepository.loadMetrics(topicName) : TopicMetrics.unavailable();
    }

    public String fetchSingleMessage(String brokersClusterName, TopicName topicName, Integer partition, Long offset) {
        return multiDCAwareService.readMessage(brokersClusterName, topicName, partition, offset);
    }

    public List<String> listTrackedTopicNames() {
        return groupService.listGroups().stream()
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
}
