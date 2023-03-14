package pl.allegro.tech.hermes.management.domain.readiness;

import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class SetReadinessCommand extends RepositoryCommand<ReadinessRepository> {
    private final DatacenterReadiness readiness;

    public SetReadinessCommand(DatacenterReadiness readiness) {
        this.readiness = readiness;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<ReadinessRepository> holder) { }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<ReadinessRepository> holder) {
        if (holder.getDatacenterName().equals(readiness.getDatacenter())) {
            holder.getRepository().setReadiness(isReady());
        }
    }

    private boolean isReady() {
        switch (readiness.getStatus()) {
            case READY:
                return true;
            case NOT_READY:
                return false;
            default:
                throw new InternalProcessingException("Invalid readiness status: " + readiness.getStatus());
        }
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<ReadinessRepository> holder) { }

    @Override
    public Class<ReadinessRepository> getRepositoryType() {
        return ReadinessRepository.class;
    }

    @Override
    public String toString() {
        return "SetReadinessCommand(" + readiness.toString() + ")";
    }
}
