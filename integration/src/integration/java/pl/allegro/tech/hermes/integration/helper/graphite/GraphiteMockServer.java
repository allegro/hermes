package pl.allegro.tech.hermes.integration.helper.graphite;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.integration.env.GraphiteMockStarter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class GraphiteMockServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteMockStarter.class);
    
    private final int port;
    
    private ServerSocket serverSocket;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    private volatile boolean listen;

    private final Map<String, Double> expectedMetrics = Maps.newHashMap();
    private final Map<String, Boolean> assertedMetrics = Maps.newConcurrentMap();

    public GraphiteMockServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        listen = true;
        handleConnections();
    }

    private void handleConnections() {
        executor.execute(() -> {
            while (listen) {
                try {
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    readData(reader);
                } catch (IOException e) {
                    LOGGER.info("Socket closed");
                }
            }
        });
    }

    public void stop() throws IOException {
        listen = false;
        serverSocket.close();
    }

    public void expectMetric(String metricNamePattern, double value) {
        LOGGER.info("Expecting to receive metric with pattern {} and value {}", metricNamePattern, value);
        expectedMetrics.put(metricNamePattern, value);
    }

    public void waitUntilReceived() {
        await().atMost(adjust(TEN_SECONDS)).until(() -> {
            for (String metric : expectedMetrics.keySet()) {
                if (assertedMetrics.get(metric) == null || !assertedMetrics.get(metric)) {
                    LOGGER.debug("mismatch metric {}", metric);
                    return false;
                }
            }
            LOGGER.info("Asserted metrics");
            return true;
        });
    }

    private void readData(final BufferedReader reader) {
        executor.execute(() -> readLines(reader));
    }

    private void readLines(BufferedReader reader) {
        String responseLine;
        try {
            while ((responseLine = reader.readLine()) != null) {
                if (StringUtils.isEmpty(responseLine)) {
                    continue;
                }
                handleMetric(MetricCreator.create(responseLine));
                LOGGER.debug("Received metric {}", responseLine);
            }
        } catch (IOException e) {
            LOGGER.error("Error while reading line", e);
        }
    }

    private void handleMetric(Metric metric) {
        for (String key : expectedMetrics.keySet()) {
            if (metric.getName().matches(key)) {
                assertedMetrics.put(key, expectedMetrics.get(key).equals(Double.valueOf(metric.getValue())));
                break;
            }
        }
    }

}
