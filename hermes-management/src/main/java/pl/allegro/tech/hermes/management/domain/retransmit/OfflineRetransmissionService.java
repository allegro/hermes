package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OfflineRetransmissionService {
    private final OfflineRetransmissionRepository offlineRetransmissionRepository;
    private final TopicRepository topicRepository;

    public OfflineRetransmissionService(OfflineRetransmissionRepository offlineRetransmissionRepository, TopicRepository topicRepository) {
        this.offlineRetransmissionRepository = offlineRetransmissionRepository;
        this.topicRepository = topicRepository;
    }

    public void validateRequest(OfflineRetransmissionRequest request) {
        TopicName sourceTopicName = TopicName.fromQualifiedName(request.getSourceTopic());
        TopicName targetTopicName = TopicName.fromQualifiedName(request.getTargetTopic());

        ensureTopicsExist(sourceTopicName, targetTopicName);
        ensureTimeRangeIsProper(request);
        ensureTopicIsNotStoredOffline(targetTopicName);
    }

    public void createTask(OfflineRetransmissionRequest request) {
        saveTask(request);
    }

    public List<OfflineRetransmissionTask> getAllTasks() {
        return offlineRetransmissionRepository.getAllTasks();
    }

    public void deleteTask(String taskId) {
        try {
            offlineRetransmissionRepository.deleteTask(taskId);
        } catch (InternalProcessingException ex) {
            if (ex.getCause() instanceof OfflineRetransmissionValidationException) {
                throw (OfflineRetransmissionValidationException) ex.getCause();
            }
            throw ex;
        }
    }

    private void ensureTopicsExist(TopicName sourceTopicName, TopicName targetTopicName) {
        boolean sourceTopicExists = topicRepository.topicExists(sourceTopicName);
        boolean targetTopicExists = topicRepository.topicExists(targetTopicName);
        if (!sourceTopicExists) {
            throw new OfflineRetransmissionValidationException("Source topic does not exist");
        }
        if (!targetTopicExists) {
            throw new OfflineRetransmissionValidationException("Target topic does not exist");
        }
    }

    private void ensureTimeRangeIsProper(OfflineRetransmissionRequest request) {
        if (request.getStartTimestamp().isAfter(request.getEndTimestamp())
                || request.getStartTimestamp().equals(request.getEndTimestamp())) {
            throw new OfflineRetransmissionValidationException("End timestamp must be greater than start timestamp");
        }
    }

    private void ensureTopicIsNotStoredOffline(TopicName targetTopicName) {
        Topic targetTopic = topicRepository.getTopicDetails(targetTopicName);
        if (targetTopic.getOfflineStorage().isEnabled()) {
            throw new OfflineRetransmissionValidationException("Target topic must not be stored offline");
        }
    }

    private void saveTask(OfflineRetransmissionRequest request) {
        OfflineRetransmissionTask task =
                new OfflineRetransmissionTask(UUID.randomUUID().toString(), request, Instant.now());
        offlineRetransmissionRepository.saveTask(task);
    }
}
