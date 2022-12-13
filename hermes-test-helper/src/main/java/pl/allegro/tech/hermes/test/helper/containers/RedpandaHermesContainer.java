package pl.allegro.tech.hermes.test.helper.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.Network;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

class RedpandaHermesContainer extends RedpandaContainer {
    private static final String START_STOP_SCRIPT = "/kafka_start_stop_wrapper.sh";
    private static final String STARTER_SCRIPT = "/kafka_start.sh";
    private static final int KAFKA_PORT = 9093;
    private static final int REDPANDA_PORT = 9092;
    private static final int KAFKA_INTERNAL_CLIENT_PORT = 9094;
    private static final Duration READINESS_CHECK_TIMEOUT = Duration.ofMinutes(360);

    private final String networkAlias;
    private final BrokerId brokerId;
    private String externalZookeeperConnect = null;
    private int advertisedPort;
    private boolean running = false;

    RedpandaHermesContainer(DockerImageName dockerImageName, Network network, int brokerNum) {
        super(dockerImageName);
        withNetwork(network);
        withExposedPorts(KAFKA_PORT);
        withExposedPorts(REDPANDA_PORT);
        this.brokerId = new BrokerId(brokerNum);
        this.networkAlias = "broker-" + brokerNum;
        withNetworkAliases(networkAlias);
    }

    BrokerId getBrokerId() {
        return brokerId;
    }

    boolean isKafkaRunning() {
        return running;
    }

    void stopKafka() {
        // todo
    }

    void startKafka() {
        // todo
    }

    private boolean isStopped() throws IOException, InterruptedException {
        String command = "ps aux | grep kafka";
        ExecResult result = execInContainer("sh", "-c", command);
        return result.getExitCode() == 0 && !result.getStdout().contains("java");
    }

    private boolean isStarted() throws IOException, InterruptedException {
        ExecResult result = execInContainer("sh", "-c", "kafka-topics --bootstrap-server localhost:9092 --list");
        return result.getExitCode() == 0;
    }

    RedpandaContainer withExternalZookeeper(String connectString) {
        externalZookeeperConnect = connectString;
        return self();
    }

    RedpandaContainer withAdvertisedPort(int advertisedPort) {
        this.advertisedPort = advertisedPort;
        return self();
    }

    String getAddressForExternalClients() {
        return String.format("%s:%s", getContainerIpAddress(), advertisedPort);
    }

    String getAddressForInternalClients() {
        return String.format("%s:%s", networkAlias, KAFKA_INTERNAL_CLIENT_PORT);
    }

//    @Override
//    protected void doStart() {
//        withCommand("sh", "-c", "while [ ! -f " + START_STOP_SCRIPT + " ]; do sleep 0.1; done; " + START_STOP_SCRIPT);
//        super.doStart();
//    }

//    @Override
//    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
//        try {
//            super.containerIsStarting(containerInfo, reused);
//            if (reused) {
//                return;
//            }
//            String startScript = readFileFromClasspath("testcontainers/kafka_start.sh")
//                    .replaceAll("<KAFKA_MAPPED_PORT>", advertisedPort + "")
//                    .replaceAll("<KAFKA_INTERNAL_CLIENT_PORT>", KAFKA_INTERNAL_CLIENT_PORT + "")
//                    .replaceAll("<BROKER_HOSTNAME>", networkAlias)
//                    .replaceAll("<ZOOKEEPER_CONNECT>", externalZookeeperConnect);
//            copyScriptToContainer(startScript, this, STARTER_SCRIPT);
//            String wrapperScript = readFileFromClasspath("testcontainers/kafka_start_stop_wrapper.sh")
//                    .replaceAll("<STARTER_SCRIPT>", STARTER_SCRIPT);
//            copyScriptToContainer(wrapperScript, this, START_STOP_SCRIPT);
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        running = true;
    }

    @Override
    protected void containerIsStopped(InspectContainerResponse containerInfo) {
        super.containerIsStopped(containerInfo);
        running = false;
    }
}
