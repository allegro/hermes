package pl.allegro.tech.hermes.management.domain.readiness;

import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.QueryCommand;

public class GetReadinessQuery extends QueryCommand<Boolean, ReadinessRepository> {

    @Override
    public Boolean query(DatacenterBoundRepositoryHolder<ReadinessRepository> holder) {
        return holder.getRepository().isReady();
    }

    @Override
    public Class<ReadinessRepository> getRepositoryType() {
        return ReadinessRepository.class;
    }
}
