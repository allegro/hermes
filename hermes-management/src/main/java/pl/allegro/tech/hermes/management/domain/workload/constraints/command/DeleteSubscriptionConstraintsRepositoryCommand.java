package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class DeleteSubscriptionConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final SubscriptionName subscriptionName;
    private Constraints backup;

    public DeleteSubscriptionConstraintsRepositoryCommand(SubscriptionName subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        backup = holder.getRepository().getConsumersWorkloadConstraints().getSubscriptionConstraints().get(subscriptionName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        holder.getRepository().deleteConstraints(subscriptionName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        if (backup != null) {
            holder.getRepository().createConstraints(subscriptionName, backup);
        }
    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }

    @Override
    public String toString() {
        return String.format("DeleteSubscriptionConstraints(%s)", subscriptionName.getQualifiedName());
    }
}
