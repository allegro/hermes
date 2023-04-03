package pl.allegro.tech.hermes.management.domain.readiness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.QueryCommand;

public class GetReadinessQuery extends QueryCommand<ReadinessStatus, ReadinessRepository> {

    private static final Logger logger = LoggerFactory.getLogger(GetReadinessQuery.class);

    @Override
    public ReadinessStatus query(DatacenterBoundRepositoryHolder<ReadinessRepository> holder) {
        try {
            boolean ready = holder.getRepository().isReady();
            return ready ? ReadinessStatus.READY : ReadinessStatus.NOT_READY;
        } catch (Exception e) {
            logger.error("Cannot obtain readiness status from {}", holder.getDatacenterName(), e);
            return ReadinessStatus.UNDEFINED;
        }
    }

    @Override
    public Class<ReadinessRepository> getRepositoryType() {
        return ReadinessRepository.class;
    }
}
