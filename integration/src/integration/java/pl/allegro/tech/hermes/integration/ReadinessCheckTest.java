package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.setup.HermesFrontendInstance;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class ReadinessCheckTest extends IntegrationTest {

    @Test
    public void shouldRespectReadinessStatusSetByAdmin() {
        // given
        HermesFrontendInstance hermesFrontend = HermesFrontendInstance.starter()
                .readinessCheckIntervalInSeconds(1)
                .zookeeperConnectionString(hermesZookeeperOne.getConnectionString())
                .kafkaConnectionString(kafkaClusterOne.getBootstrapServersForExternalClients())
                .start();

        // when
        managementStarter.operations().setReadiness(DC1, false);

        // then
        await().atMost(5, SECONDS).until(() -> {
            assertThat(hermesFrontend.isReady()).isFalse();
        });

        // when
        managementStarter.operations().setReadiness(DC1, true);

        // then
        await().atMost(5, SECONDS).until(() -> {
            assertThat(hermesFrontend.isReady()).isTrue();
        });

        // cleanup
        hermesFrontend.stop();
    }
}
