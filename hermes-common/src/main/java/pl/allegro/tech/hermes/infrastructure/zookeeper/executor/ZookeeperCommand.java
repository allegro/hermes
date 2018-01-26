package pl.allegro.tech.hermes.infrastructure.zookeeper.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;

public abstract class ZookeeperCommand {

    protected final ZookeeperPaths paths;

    protected ZookeeperCommand(ZookeeperPaths paths) {
        this.paths = paths;
    }

    public abstract void backup(ZookeeperClient client);

    public abstract void execute(ZookeeperClient client);

    public abstract void rollback(ZookeeperClient client);

    protected byte[] marshall(ObjectMapper mapper, Object object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new InternalProcessingException("Could not marshall " + object.getClass().getSimpleName(), e);
        }
    }
}
