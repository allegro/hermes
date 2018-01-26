package pl.allegro.tech.hermes.infrastructure.zookeeper.executor;

import java.util.List;

public class ZookeeperCommandFailedException extends RuntimeException {
    private List<Throwable> causes;

    public ZookeeperCommandFailedException(String message, List<Throwable> causes) {
        super(message, causes.isEmpty() ? null : causes.get(0));
        this.causes = causes;
    }

    public ZookeeperCommandFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public List<Throwable> getCauses() {
        return causes;
    }
}
