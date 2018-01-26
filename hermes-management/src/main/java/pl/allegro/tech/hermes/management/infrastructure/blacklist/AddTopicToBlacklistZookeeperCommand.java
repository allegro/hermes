package pl.allegro.tech.hermes.management.infrastructure.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class AddTopicToBlacklistZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(AddTopicToBlacklistZookeeperCommand.class);

    private final String qualifiedTopicName;

    AddTopicToBlacklistZookeeperCommand(String qualifiedTopicName, ZookeeperPaths paths) {
        super(paths);
        this.qualifiedTopicName = qualifiedTopicName;
    }

    @Override
    public void backup(ZookeeperClient client) {}

    @Override
    public void execute(ZookeeperClient client) {
        logger.info("Adding topic {} to Blacklist", qualifiedTopicName, client.getName());

        client.ensurePathExists(paths.blacklistedTopicPath(qualifiedTopicName));
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: topic {} blacklist addition via client {}", qualifiedTopicName,
                client.getName());
        client.deleteWithChildren(paths.blacklistedTopicPath(qualifiedTopicName));
    }
}
