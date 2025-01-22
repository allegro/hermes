package pl.allegro.tech.hermes.consumers.supervisor;

import java.time.Duration;

public interface SupervisorParameters {

  Duration getInterval();

  Duration getUnhealthyAfter();

  Duration getKillAfter();
}
