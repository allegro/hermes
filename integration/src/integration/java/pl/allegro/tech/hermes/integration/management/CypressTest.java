package pl.allegro.tech.hermes.integration.management;

import io.github.wimdeblauwe.testcontainers.cypress.CypressContainer;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestResults;
import io.github.wimdeblauwe.testcontainers.cypress.MochawesomeGatherTestResultsStrategy;
import org.testcontainers.Testcontainers;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

public class CypressTest extends IntegrationTest {

    private final MochawesomeGatherTestResultsStrategy gradleTestResultStrategy = new MochawesomeGatherTestResultsStrategy(
            FileSystems.getDefault().getPath("build", "resources", "integration", "e2e", "cypress", "reports", "mochawesome"));

    @Test
    public void allCypressSpecsShouldPass() throws IOException, InterruptedException, TimeoutException {
        Testcontainers.exposeHostPorts(MANAGEMENT_PORT);
        try (CypressContainer container = new CypressContainer()
                .withGatherTestResultsStrategy(gradleTestResultStrategy)
                .withLocalServerPort(MANAGEMENT_PORT)) {

            // run tests
            container.start();

            CypressTestResults testResults = container.getTestResults();
            assertThat(testResults.getNumberOfFailingTests())
                    .withFailMessage("There was a failure running the Cypress tests!\n\n%s\n\nLogs:%s", testResults, container.getLogs())
                    .isEqualTo(0);
        }
    }

}
