package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class DeleteSubscriptionConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final SubscriptionName subscriptionName;

    public DeleteSubscriptionConstraintsRepositoryCommand(SubscriptionName subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    public void backup(WorkloadConstraintsRepository repository) {

    }

    @Override
    public void execute(WorkloadConstraintsRepository repository) {
        repository.deleteConstraints(subscriptionName);
    }

    @Override
    public void rollback(WorkloadConstraintsRepository repository) {

    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }
}
