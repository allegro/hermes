package pl.allegro.tech.hermes.infrastructure.zookeeper.client;

public class ZookeeperClientNotFoundException extends RuntimeException {
    public ZookeeperClientNotFoundException(String message) {
        super(message);
    }
}
