package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class DeleteTopicConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final TopicName topicName;
    private Constraints backup;

    public DeleteTopicConstraintsRepositoryCommand(TopicName topicName) {
        this.topicName = topicName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        backup = holder.getRepository().getConsumersWorkloadConstraints().getTopicConstraints().get(topicName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        holder.getRepository().deleteConstraints(topicName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        if (backup != null) {
            holder.getRepository().createConstraints(topicName, backup);
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
