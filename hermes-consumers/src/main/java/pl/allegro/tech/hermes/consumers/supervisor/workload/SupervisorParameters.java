package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.time.Duration;

public interface SupervisorParameters {

    Duration getInterval();

    Duration getUnhealthyAfter();

    Duration getKillAfter();
}
