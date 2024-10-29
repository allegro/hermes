package pl.allegro.tech.hermes.integrationtests.management;

import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.READY;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class ReadinessManagementTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldNotFailWhileSettingReadinessStatusForTheFirstTime() {
    // when
    hermes.api().setReadiness("unhealthy-dc", false);

    // then
    hermes
        .api()
        .getReadiness()
        .expectStatus()
        .isOk()
        .expectBodyList(DatacenterReadiness.class)
        // 'unhealthy-dc' should not be returned here, since it doesn't exist in management
        // configuration
        // In this test, we are just verifying if setting readiness status for the first time
        // doesn't break anything.
        .hasSize(1)
        .contains(new DatacenterReadiness(DEFAULT_DC_NAME, READY));
  }
}
