package pl.allegro.tech.hermes.management.infrastructure.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;

import java.util.List;

public class DistributedZookeeperTopicBlacklistRepository extends DistributedZookeeperRepository
        implements TopicBlacklistRepository {
    private final ZookeeperPaths paths;

    public DistributedZookeeperTopicBlacklistRepository(ZookeeperClientManager clientManager,
                                                           ZookeeperCommandExecutor commandExecutor,
                                                           ZookeeperPaths paths,
                                                           ObjectMapper mapper) {
        super(clientManager, commandExecutor, mapper);
        this.paths = paths;
    }

    @Override
    public void add(String qualifiedTopicName) {
        ZookeeperCommand command = new AddTopicToBlacklistZookeeperCommand(qualifiedTopicName, paths);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void remove(String qualifiedTopicName) {
        ZookeeperCommand command = new RemoveTopicFromBlacklistZookeeperCommand(qualifiedTopicName, paths);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public boolean isBlacklisted(String qualifiedTopicName) {
        ZookeeperClient client = clientManager.getLocalClient();
        String path = paths.blacklistedTopicPath(qualifiedTopicName);
        return client.pathExists(path);
    }

    @Override
    public List<String> list() {
        ZookeeperClient client = clientManager.getLocalClient();
        return client.childrenOf(paths.topicsBlacklistPath());
    }
}
