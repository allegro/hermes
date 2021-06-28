package pl.allegro.tech.hermes.management.domain.readiness;

import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;


public class ReadinessService {
    private final MultiDatacenterRepositoryCommandExecutor commandExecutor;

    public ReadinessService(MultiDatacenterRepositoryCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public void setReady(DatacenterReadiness datacenterReadiness) {
        commandExecutor.execute(new SetReadinessCommand(datacenterReadiness));
    }
}
