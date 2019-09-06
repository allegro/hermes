package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateTopicConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final TopicName topicName;
    private final Constraints constraints;
    private boolean exist;

    public CreateTopicConstraintsRepositoryCommand(TopicName topicName, Constraints constraints) {
        this.topicName = topicName;
        this.constraints = constraints;
    }

    @Override
    public void backup(WorkloadConstraintsRepository repository) {
        exist = repository.constraintsExist(topicName);
    }

    @Override
    public void execute(WorkloadConstraintsRepository repository) {
        repository.createConstraints(topicName, constraints);
    }

    @Override
    public void rollback(WorkloadConstraintsRepository repository) {
        if (!exist) {
            repository.deleteConstraints(topicName);
        }
    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }

    @Override
    public String toString() {
        return String.format("CreateTopicConstraints(%s)", topicName.qualifiedName());
    }
}
