package pl.allegro.tech.hermes.management.domain.readiness;

import java.util.List;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryQueryExecutor;


public class ReadinessService {
    private final MultiDatacenterRepositoryCommandExecutor commandExecutor;
    private final MultiDatacenterRepositoryQueryExecutor queryExecutor;

    public ReadinessService(MultiDatacenterRepositoryCommandExecutor commandExecutor,
                            MultiDatacenterRepositoryQueryExecutor queryExecutor) {
        this.commandExecutor = commandExecutor;
        this.queryExecutor = queryExecutor;
    }

    public void setReady(DatacenterReadiness datacenterReadiness) {
        commandExecutor.execute(new SetReadinessCommand(datacenterReadiness));
    }

    public List<DatacenterReadiness> getDatacentersReadinesses() {
        return queryExecutor.execute(new GetReadinessQuery()).stream()
                .map(r -> new DatacenterReadiness(r.getDatacenterName(), r.getResult()))
                .collect(Collectors.toList());
    }
}
