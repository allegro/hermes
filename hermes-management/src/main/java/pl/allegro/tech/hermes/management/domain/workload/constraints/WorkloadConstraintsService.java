package pl.allegro.tech.hermes.management.domain.workload.constraints;

import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
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

    public WorkloadConstraintsService(WorkloadConstraintsRepository workloadConstraintsRepository,
                                      MultiDatacenterRepositoryCommandExecutor commandExecutor) {
        this.workloadConstraintsRepository = workloadConstraintsRepository;
        this.commandExecutor = commandExecutor;
    }

    public ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
        return workloadConstraintsRepository.getConsumersWorkloadConstraints();
    }

    public void createConstraints(TopicName topicName, Constraints constraints) {
        commandExecutor.execute(new CreateTopicConstraintsRepositoryCommand(topicName, constraints));
    }

    public void createConstraints(SubscriptionName subscriptionName, Constraints constraints) {
        commandExecutor.execute(new CreateSubscriptionConstraintsRepositoryCommand(subscriptionName, constraints));
    }

    public void updateConstraints(TopicName topicName, Constraints constraints) {
        commandExecutor.execute(new UpdateTopicConstraintsRepositoryCommand(topicName, constraints));
    }

    public void updateConstraints(SubscriptionName subscriptionName, Constraints constraints) {
        commandExecutor.execute(new UpdateSubscriptionConstraintsRepositoryCommand(subscriptionName, constraints));
    }

    public void deleteConstraints(TopicName topicName) {
        commandExecutor.execute(new DeleteTopicConstraintsRepositoryCommand(topicName));
    }

    public void deleteConstraints(SubscriptionName subscriptionName) {
        commandExecutor.execute(new DeleteSubscriptionConstraintsRepositoryCommand(subscriptionName));
    }

    public boolean constraintsExist(TopicName topicName) {
        return workloadConstraintsRepository.constraintsExist(topicName);
    }

    public boolean constraintsExist(SubscriptionName subscriptionName) {
        return workloadConstraintsRepository.constraintsExist(subscriptionName);
    }
}
