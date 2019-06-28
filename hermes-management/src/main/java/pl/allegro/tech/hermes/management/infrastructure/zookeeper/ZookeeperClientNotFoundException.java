package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

public class ZookeeperClientNotFoundException extends RuntimeException {
    public ZookeeperClientNotFoundException(String localDcName) {
        super("No Zookeeper client is configured to connect to cluster on DC (name: " + localDcName + ").");
    }
}
