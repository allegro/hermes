package pl.allegro.tech.hermes.management.domain.workload.constraints.command;

import pl.allegro.tech.hermes.api.Constraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateSubscriptionConstraintsRepositoryCommand extends RepositoryCommand<WorkloadConstraintsRepository> {

    private final SubscriptionName subscriptionName;
    private final Constraints constraints;
    private boolean exists;

    public CreateSubscriptionConstraintsRepositoryCommand(SubscriptionName subscriptionName, Constraints constraints) {
        this.subscriptionName = subscriptionName;
        this.constraints = constraints;
    }

    @Override
    public void backup(WorkloadConstraintsRepository repository) {
        exists = repository.constraintsExist(subscriptionName);
    }

    @Override
    public void execute(WorkloadConstraintsRepository repository) {
        repository.createConstraints(subscriptionName, constraints);
    }

    @Override
    public void rollback(WorkloadConstraintsRepository repository) {
        if (!exists) {
            repository.deleteConstraints(subscriptionName);
        }
    }

    @Override
    public Class<WorkloadConstraintsRepository> getRepositoryType() {
        return WorkloadConstraintsRepository.class;
    }

    @Override
    public String toString() {
        return String.format("CreateSubscriptionConstraints(%s)", subscriptionName.getQualifiedName());
    }
}
