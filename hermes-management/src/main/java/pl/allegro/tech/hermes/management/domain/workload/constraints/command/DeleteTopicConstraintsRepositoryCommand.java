package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class DeleteTopicConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final TopicName topicName;
    private Constraints backup;

    public DeleteTopicConstraintsRepositoryCommand(TopicName topicName) {
        this.topicName = topicName;
    }

    @Override
    public void backup(WorkloadConstraintsRepository repository) {
        backup = repository.getConsumersWorkloadConstraints().getTopicConstraints().get(topicName);
    }

    @Override
    public void execute(WorkloadConstraintsRepository repository) {
        repository.deleteConstraints(topicName);
    }

    @Override
    public void rollback(WorkloadConstraintsRepository repository) {
        if (backup != null) {
            repository.createConstraints(topicName, backup);
        }
    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }

    @Override
    public String toString() {
        return String.format("DeleteTopicConstraints(%s)", topicName.qualifiedName());
    }
}
