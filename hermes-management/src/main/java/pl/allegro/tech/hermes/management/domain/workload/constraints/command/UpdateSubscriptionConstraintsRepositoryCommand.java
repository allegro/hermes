package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateSubscriptionConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final SubscriptionName subscriptionName;
    private final Constraints constraints;

    public UpdateSubscriptionConstraintsRepositoryCommand(SubscriptionName subscriptionName, Constraints constraints) {
        this.subscriptionName = subscriptionName;
        this.constraints = constraints;
    }

    @Override
    public void backup(WorkloadConstraintsRepository repository) {

    }

    @Override
    public void execute(WorkloadConstraintsRepository repository) {
        repository.updateConstraints(subscriptionName, constraints);
    }

    @Override
    public void rollback(WorkloadConstraintsRepository repository) {

    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }
}
