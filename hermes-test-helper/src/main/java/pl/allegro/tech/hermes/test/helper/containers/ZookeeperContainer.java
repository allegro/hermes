package pl.allegro.tech.hermes.test.helper.containers;

import java.util.Optional;
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

  private void setupLogger() {
    if (this.logger != null) {
      Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(this.logger);
      followOutput(logConsumer);
    }
  }

  public String getConnectionString() {
    return String.format("%s:%s", getHost(), getMappedPort(clientPort));
  }
}
