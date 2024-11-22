package pl.allegro.tech.hermes.test.helper.containers;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.test.helper.containers.TestcontainersUtils.copyScriptToContainer;
import static pl.allegro.tech.hermes.test.helper.containers.TestcontainersUtils.readFileFromClasspath;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ZookeeperContainer extends GenericContainer<ZookeeperContainer> {
  private static final DockerImageName DEFAULT_ZOOKEEPER_IMAGE_NAME =
      DockerImageName.parse("confluentinc/cp-zookeeper").withTag(ImageTags.confluentImagesTag());
  private static final int DEFAULT_ZOOKEEPER_PORT = 2181;
  private static final Duration ZOOKEEPER_RESTART_WAIT_TIMEOUT = Duration.ofSeconds(30);
  private static final String START_STOP_SCRIPT = "/zookeeper_start_stop_wrapper.sh";
  private Logger logger;

  private final int clientPort;

  public ZookeeperContainer() {
    this(DEFAULT_ZOOKEEPER_IMAGE_NAME, DEFAULT_ZOOKEEPER_PORT);
  }

  public ZookeeperContainer(String loggerName) {
    this(DEFAULT_ZOOKEEPER_IMAGE_NAME, DEFAULT_ZOOKEEPER_PORT, Optional.of(loggerName));
  }

  public ZookeeperContainer(DockerImageName zooKeeperImage, int clientPort) {
    this(zooKeeperImage, clientPort, Optional.empty());
  }

  private ZookeeperContainer(
      DockerImageName zooKeeperImage, int clientPort, Optional<String> maybeLoggerName) {
    super(zooKeeperImage);
    maybeLoggerName.ifPresent(s -> this.logger = LoggerFactory.getLogger(s));
    withExposedPorts(clientPort);
    withEnv("ZOOKEEPER_CLIENT_PORT", String.valueOf(clientPort));
    this.clientPort = clientPort;
  }

  @Override
  public void start() {
    super.start();
    waitingFor(Wait.forHealthcheck());
    setupLogger();
  }

  @Override
  protected void doStart() {
    withCommand(
        "sh",
        "-c",
        "while [ ! -f " + START_STOP_SCRIPT + " ]; do sleep 0.1; done; " + START_STOP_SCRIPT);
    super.doStart();
  }

  private void setupLogger() {
    if (this.logger != null) {
      Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(this.logger);
      followOutput(logConsumer);
    }
  }

  public String getConnectionString() {
    return format("%s:%s", getHost(), getMappedPort(clientPort));
  }

  @Override
  protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
    try {
      super.containerIsStarting(containerInfo, reused);
      if (reused) {
        return;
      }
      String wrapperScript =
          readFileFromClasspath("testcontainers/zookeeper_start_stop_wrapper.sh");
      copyScriptToContainer(wrapperScript, this, START_STOP_SCRIPT);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void restart() throws IOException, InterruptedException {
    logger.info(
        "Restarting zookeeper process with exposed container port {}", getMappedPort(clientPort));
    stopAndWaitUntilKilled();
    startAndWaitUntilRunning();
  }

  private void startAndWaitUntilRunning() throws IOException, InterruptedException {
    // try really hard to start this process as it can have temporary issues colliding with previous
    // process
    int startsTrials = 3;
    for (int i = 0; i < startsTrials; i++) {
      execInContainer("sh", "-c", "touch /tmp/start");
      Unreliables.retryUntilTrue(
          (int) ZOOKEEPER_RESTART_WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS, this::isStarted);
    }
  }

  private void stopAndWaitUntilKilled() throws IOException, InterruptedException {
    execInContainer("sh", "-c", "touch /tmp/stop");
    Unreliables.retryUntilTrue(
        (int) ZOOKEEPER_RESTART_WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS, this::isStopped);
  }

  public boolean isStopped() throws IOException, InterruptedException {
    ExecResult result =
        execInContainer("sh", "-c", format("echo srvr | nc localhost %s", DEFAULT_ZOOKEEPER_PORT));
    return result.getExitCode() != 0;
  }

  public boolean isStarted() throws IOException, InterruptedException {
    ExecResult result =
        execInContainer("sh", "-c", format("echo srvr | nc localhost %s", DEFAULT_ZOOKEEPER_PORT));
    return result.getExitCode() == 0;
  }
}
