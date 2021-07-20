package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateSubscriptionConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final SubscriptionName subscriptionName;
    private final Constraints constraints;
    private Constraints backup;

    public UpdateSubscriptionConstraintsRepositoryCommand(SubscriptionName subscriptionName, Constraints constraints) {
        this.subscriptionName = subscriptionName;
        this.constraints = constraints;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        backup = holder.getRepository().getConsumersWorkloadConstraints().getSubscriptionConstraints().get(subscriptionName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        holder.getRepository().updateConstraints(subscriptionName, constraints);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<WorkloadConstraintsRepository> holder) {
        if (backup != null) {
            holder.getRepository().updateConstraints(subscriptionName, backup);
        }
    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }

    @Override
    public String toString() {
        return String.format("UpdateSubscriptionConstraints(%s)", subscriptionName.getQualifiedName());
    }
}
