package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateTopicConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final TopicName topicName;
    private final Constraints constraints;
    private Constraints backup;

    public UpdateTopicConstraintsRepositoryCommand(TopicName topicName, Constraints constraints) {
        this.topicName = topicName;
        this.constraints = constraints;
    }

    @Override
    public void backup(WorkloadConstraintsRepository repository) {
        backup = repository.getConsumersWorkloadConstraints().getTopicConstraints().get(topicName);
    }

    @Override
    public void execute(WorkloadConstraintsRepository repository) {
        repository.updateConstraints(topicName, constraints);
    }

    @Override
    public void rollback(WorkloadConstraintsRepository repository) {
        if (backup != null) {
            repository.updateConstraints(topicName, backup);
        }
    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }

    @Override
    public String toString() {
        return String.format("UpdateTopicConstraints(%s)", topicName.qualifiedName());
    }
}
