package pl.allegro.tech.hermes.test.helper.containers;

import static pl.allegro.tech.hermes.test.helper.containers.TestcontainersUtils.copyScriptToContainer;
import static pl.allegro.tech.hermes.test.helper.containers.TestcontainersUtils.readFileFromClasspath;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

class KafkaContainer extends GenericContainer<KafkaContainer> {
  private static final String START_STOP_SCRIPT = "/kafka_start_stop_wrapper.sh";
  private static final String STARTER_SCRIPT = "/kafka_start.sh";
  private static final int KAFKA_PORT = 9093;
  private static final int KAFKA_INTERNAL_CLIENT_PORT = 9094;
  private static final Duration READINESS_CHECK_TIMEOUT = Duration.ofMinutes(360);

  private final String networkAlias;
  private final BrokerId brokerId;
  private String externalZookeeperConnect = null;
  private int advertisedPort;
  private boolean running = false;

  KafkaContainer(DockerImageName dockerImageName, Network network, int brokerNum) {
    super(dockerImageName);
    withNetwork(network);
    withExposedPorts(KAFKA_PORT);
    this.brokerId = new BrokerId(brokerNum);
    this.networkAlias = "broker-" + brokerNum;
    withNetworkAliases(networkAlias);
    withEnv("KAFKA_BROKER_ID", "" + brokerNum);
    withEnv(
        "KAFKA_LISTENERS",
        "PLAINTEXT://0.0.0.0:"
            + KAFKA_PORT
            + ",BROKER://0.0.0.0:9092,INTERNAL_CLIENT://0.0.0.0:"
            + KAFKA_INTERNAL_CLIENT_PORT);
    withEnv(
        "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
        "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,INTERNAL_CLIENT:PLAINTEXT");
    withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
    withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
    withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
    withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
    withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
  }

  BrokerId getBrokerId() {
    return brokerId;
  }

  boolean isKafkaRunning() {
    return running;
  }

  void stopKafka() {
    try {
      ExecResult execResult = execInContainer("sh", "-c", "touch /tmp/stop");
      if (execResult.getExitCode() != 0) {
        throw new Exception(execResult.getStderr());
      }
      Unreliables.retryUntilTrue(
          (int) READINESS_CHECK_TIMEOUT.getSeconds(), TimeUnit.SECONDS, this::isStopped);
      running = false;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  void startKafka() {
    try {
      ExecResult execResult = execInContainer("sh", "-c", "touch /tmp/start");
      if (execResult.getExitCode() != 0) {
        throw new Exception(execResult.getStderr());
      }
      Unreliables.retryUntilTrue(
          (int) READINESS_CHECK_TIMEOUT.getSeconds(), TimeUnit.SECONDS, this::isStarted);
      running = true;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private boolean isStopped() throws IOException, InterruptedException {
    String command = "ps aux | grep kafka";
    ExecResult result = execInContainer("sh", "-c", command);
    return result.getExitCode() == 0 && !result.getStdout().contains("java");
  }

  private boolean isStarted() throws IOException, InterruptedException {
    ExecResult result =
        execInContainer("sh", "-c", "kafka-topics --bootstrap-server localhost:9092 --list");
    return result.getExitCode() == 0;
  }

  KafkaContainer withExternalZookeeper(String connectString) {
    externalZookeeperConnect = connectString;
    return self();
  }

  KafkaContainer withAdvertisedPort(int advertisedPort) {
    this.advertisedPort = advertisedPort;
    return self();
  }

  String getAddressForExternalClients() {
    return String.format("%s:%s", getHost(), advertisedPort);
  }

  String getAddressForInternalClients() {
    return String.format("%s:%s", networkAlias, KAFKA_INTERNAL_CLIENT_PORT);
  }

  @Override
  protected void doStart() {
    withCommand(
        "sh",
        "-c",
        "while [ ! -f " + START_STOP_SCRIPT + " ]; do sleep 0.1; done; " + START_STOP_SCRIPT);
    super.doStart();
  }

  @Override
  protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
    try {
      super.containerIsStarting(containerInfo, reused);
      if (reused) {
        return;
      }
      String startScript =
          readFileFromClasspath("testcontainers/kafka_start.sh")
              .replaceAll("<KAFKA_MAPPED_PORT>", advertisedPort + "")
              .replaceAll("<KAFKA_INTERNAL_CLIENT_PORT>", KAFKA_INTERNAL_CLIENT_PORT + "")
              .replaceAll("<BROKER_HOSTNAME>", networkAlias)
              .replaceAll("<ZOOKEEPER_CONNECT>", externalZookeeperConnect);
      copyScriptToContainer(startScript, this, STARTER_SCRIPT);
      String wrapperScript =
          readFileFromClasspath("testcontainers/kafka_start_stop_wrapper.sh")
              .replaceAll("<STARTER_SCRIPT>", STARTER_SCRIPT);
      copyScriptToContainer(wrapperScript, this, START_STOP_SCRIPT);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

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
