package pl.allegro.tech.hermes.management.domain.workload.constraints;

import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.workload.constraints.command.CreateSubscriptionConstraintsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.workload.constraints.command.CreateTopicConstraintsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.workload.constraints.command.DeleteSubscriptionConstraintsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.workload.constraints.command.DeleteTopicConstraintsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.workload.constraints.command.UpdateSubscriptionConstraintsRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.workload.constraints.command.UpdateTopicConstraintsRepositoryCommand;

@Service
public class WorkloadConstraintsService {

  private final WorkloadConstraintsRepository workloadConstraintsRepository;
  private final MultiDatacenterRepositoryCommandExecutor commandExecutor;

  public WorkloadConstraintsService(
      WorkloadConstraintsRepository workloadConstraintsRepository,
      MultiDatacenterRepositoryCommandExecutor commandExecutor) {
    this.workloadConstraintsRepository = workloadConstraintsRepository;
    this.commandExecutor = commandExecutor;
  }

  public ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
    return workloadConstraintsRepository.getConsumersWorkloadConstraints();
  }

  public void createConstraints(
      TopicName topicName, Constraints constraints, RequestUser requester) {
    commandExecutor.executeByUser(
        new CreateTopicConstraintsRepositoryCommand(topicName, constraints), requester);
  }

  public void createConstraints(
      SubscriptionName subscriptionName, Constraints constraints, RequestUser requester) {
    commandExecutor.executeByUser(
        new CreateSubscriptionConstraintsRepositoryCommand(subscriptionName, constraints),
        requester);
  }

  public void updateConstraints(
      TopicName topicName, Constraints constraints, RequestUser requester) {
    commandExecutor.executeByUser(
        new UpdateTopicConstraintsRepositoryCommand(topicName, constraints), requester);
  }

  public void updateConstraints(
      SubscriptionName subscriptionName, Constraints constraints, RequestUser requester) {
    commandExecutor.executeByUser(
        new UpdateSubscriptionConstraintsRepositoryCommand(subscriptionName, constraints),
        requester);
  }

  public void deleteConstraints(TopicName topicName, RequestUser requester) {
    commandExecutor.executeByUser(
        new DeleteTopicConstraintsRepositoryCommand(topicName), requester);
  }

  public void deleteConstraints(SubscriptionName subscriptionName, RequestUser requester) {
    commandExecutor.executeByUser(
        new DeleteSubscriptionConstraintsRepositoryCommand(subscriptionName), requester);
  }

  public boolean constraintsExist(TopicName topicName) {
    return workloadConstraintsRepository.constraintsExist(topicName);
  }

  public boolean constraintsExist(SubscriptionName subscriptionName) {
    return workloadConstraintsRepository.constraintsExist(subscriptionName);
  }
}
