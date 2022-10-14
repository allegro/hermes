package pl.allegro.tech.hermes.test.helper.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ZookeeperContainer extends GenericContainer<ZookeeperContainer> {
    private static final DockerImageName DEFAULT_ZOOKEEPER_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-zookeeper")
            .withTag(ImageTags.confluentImagesTag());
    private static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    private final int clientPort;

    public ZookeeperContainer() {
        this(DEFAULT_ZOOKEEPER_IMAGE_NAME, DEFAULT_ZOOKEEPER_PORT);
    }

    public ZookeeperContainer(DockerImageName zooKeeperImage, int clientPort) {
        super(zooKeeperImage);
        withExposedPorts(clientPort);
        withEnv("ZOOKEEPER_CLIENT_PORT", String.valueOf(clientPort));
        this.clientPort = clientPort;
    }

    @Override
    public void start() {
        super.start();
        waitingFor(Wait.forHealthcheck());
    }

    public String getConnectionString() {
        return String.format("%s:%s", getContainerIpAddress(), getMappedPort(clientPort));
    }
}
