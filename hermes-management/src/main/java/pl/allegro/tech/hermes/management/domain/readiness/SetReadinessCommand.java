package pl.allegro.tech.hermes.management.domain.readiness;

import pl.allegro.tech.hermes.domain.readiness.DatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class SetReadinessCommand extends RepositoryCommand<DatacenterReadinessRepository> {
    private final DatacenterReadiness readiness;

    public SetReadinessCommand(DatacenterReadiness readiness) {
        this.readiness = readiness;
    }

    @Override
    public void backup(DatacenterReadinessRepository repository) { }

    @Override
    public void execute(DatacenterReadinessRepository repository) {
        if (repository.datacenterMatches(readiness.getDatacenter())) {
            repository.setReadiness(readiness.isReady());
        }
    }

    @Override
    public void rollback(DatacenterReadinessRepository repository) { }

    @Override
    public Class<DatacenterReadinessRepository> getRepositoryType() {
        return DatacenterReadinessRepository.class;
    }

    @Override
    public String toString() {
        return "SetReadinessCommand(" + readiness.toString() + ")";
    }
}
