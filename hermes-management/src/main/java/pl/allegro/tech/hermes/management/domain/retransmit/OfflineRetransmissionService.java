package pl.allegro.tech.hermes.management.domain.retransmit;

import static pl.allegro.tech.hermes.api.OfflineRetransmissionRequest.RetransmissionType.TOPIC;
import static pl.allegro.tech.hermes.api.OfflineRetransmissionRequest.RetransmissionType.VIEW;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pl.allegro.tech.hermes.api.OfflineRetransmissionFromTopicRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionFromViewRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

public class OfflineRetransmissionService {
  private final OfflineRetransmissionRepository offlineRetransmissionRepository;
  private final TopicRepository topicRepository;

  public OfflineRetransmissionService(
      OfflineRetransmissionRepository offlineRetransmissionRepository,
      TopicRepository topicRepository) {
    this.offlineRetransmissionRepository = offlineRetransmissionRepository;
    this.topicRepository = topicRepository;
  }

  public void validateTopicRequest(OfflineRetransmissionFromTopicRequest request) {
    TopicName sourceTopicName = TopicName.fromQualifiedName(request.getSourceTopic());
    TopicName targetTopicName = TopicName.fromQualifiedName(request.getTargetTopic());

    ensureSourceTopicExists(sourceTopicName);
    ensureTargetTopicExists(targetTopicName);
    ensureTopicIsNotStoredOffline(targetTopicName);
  }

  public void validateViewRequest(OfflineRetransmissionFromViewRequest request) {
    TopicName targetTopicName = TopicName.fromQualifiedName(request.getTargetTopic());
    ensureTargetTopicExists(targetTopicName);
    ensureTopicIsNotStoredOffline(targetTopicName);
  }

  public OfflineRetransmissionTask createTopicTask(OfflineRetransmissionFromTopicRequest request) {
    OfflineRetransmissionTask task =
        new OfflineRetransmissionTask(
            TOPIC,
            UUID.randomUUID().toString(),
            null,
            request.getSourceTopic(),
            request.getTargetTopic(),
            request.getStartTimestamp(),
            request.getEndTimestamp(),
            Instant.now());
    offlineRetransmissionRepository.saveTask(task);
    return task;
  }

  public OfflineRetransmissionTask createViewTask(OfflineRetransmissionFromViewRequest request) {
    OfflineRetransmissionTask task =
        new OfflineRetransmissionTask(
            VIEW,
            UUID.randomUUID().toString(),
            request.getSourceViewPath(),
            null,
            request.getTargetTopic(),
            null,
            null,
            Instant.now());
    offlineRetransmissionRepository.saveTask(task);
    return task;
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

  private void ensureSourceTopicExists(TopicName sourceTopicName) {
    if (!topicRepository.topicExists(sourceTopicName)) {
      throw new OfflineRetransmissionValidationException("Source topic does not exist");
    }
  }

  private void ensureTargetTopicExists(TopicName targetTopicName) {
    if (!topicRepository.topicExists(targetTopicName)) {
      throw new OfflineRetransmissionValidationException("Target topic does not exist");
    }
  }

  private void ensureTopicIsNotStoredOffline(TopicName targetTopicName) {
    Topic targetTopic = topicRepository.getTopicDetails(targetTopicName);
    if (targetTopic.getOfflineStorage().isEnabled()) {
      throw new OfflineRetransmissionValidationException("Target topic must not be stored offline");
    }
  }
}
