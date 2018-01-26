package pl.allegro.tech.hermes.infrastructure.zookeeper.executor;

import java.util.List;

public class RollbackFailedException extends ZookeeperCommandFailedException {

    public RollbackFailedException(List<String> clients, List<Throwable> causes) {
        super("Failed to rollback command on clients: " + String.join(", ", clients), causes);
    }
}
