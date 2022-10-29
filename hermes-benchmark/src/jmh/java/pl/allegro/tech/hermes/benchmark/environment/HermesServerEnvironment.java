package pl.allegro.tech.hermes.benchmark.environment;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.server.HermesServer;

import java.io.IOException;
import java.util.Objects;

@State(Scope.Benchmark)
public class HermesServerEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(HermesServerEnvironment.class);
    private static final int MAX_CONNECTIONS_PER_ROUTE = 200;
    
    public static final String BENCHMARK_TOPIC = "bench.topic";

    private HermesPublisher publisher;
    private MetricRegistry metricRegistry;

    private HermesServer hermesServer;

    public static void main(String[] args) throws Exception {
        new HermesServerEnvironment().setupEnvironment();
    }

    @Setup(Level.Trial)
    public void setupEnvironment() throws Exception {
        hermesServer = HermesServerFactory.provideHermesServer();
        hermesServer.start();
    }

    @Setup(Level.Trial)
    public void setupPublisher() throws Exception {
        metricRegistry = new MetricRegistry();

        String messageBody = loadMessageResource("completeMessage");
        publisher = new HermesPublisher(MAX_CONNECTIONS_PER_ROUTE, "http://localhost:8080/topics/" + BENCHMARK_TOPIC, messageBody, metricRegistry);
    }

    @TearDown(Level.Trial)
    public void shutdownServers() throws Exception {
        hermesServer.stop();
    }

    @TearDown(Level.Trial)
    public void shutdownPublisherAndReportMetrics() throws Exception {
        reportMetrics();
        publisher.stop();
    }

    public HermesPublisher publisher() {
        return publisher;
    }

    public static String loadMessageResource(String name) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(HermesServerEnvironment.class
            .getResourceAsStream(String.format("/message/%s.json", name))));
    }

    private void reportMetrics() {
        metricRegistry.getCounters().forEach((key, value) -> logger.info(key + ": " + value.getCount()));
    }
}
