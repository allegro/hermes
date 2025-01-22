package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import java.util.List;

public interface ConsumerGroupToDeleteRepository {
  void scheduleConsumerGroupToDeleteTask(ConsumerGroupToDelete consumerGroupToDelete);

  void deleteConsumerGroupToDeleteTask(ConsumerGroupToDelete consumerGroupToDelete);

  List<ConsumerGroupToDelete> getAllConsumerGroupsToDelete();
}
