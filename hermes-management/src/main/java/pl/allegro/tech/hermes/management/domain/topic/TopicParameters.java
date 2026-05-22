package pl.allegro.tech.hermes.management.domain.topic;

public interface TopicParameters {

  boolean isAllowRemoval();

  int getSubscriptionsAssignmentsCompletedTimeoutSeconds();

  boolean isTouchSchedulerEnabled();

  int getTouchDelayInSeconds();

  boolean isRemoveSchema();
}
