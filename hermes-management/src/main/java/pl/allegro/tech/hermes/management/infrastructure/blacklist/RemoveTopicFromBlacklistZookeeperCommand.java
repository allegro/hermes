package pl.allegro.tech.hermes.management.infrastructure.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.management.domain.blacklist.NotUnblacklistedException;

class RemoveTopicFromBlacklistZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(RemoveTopicFromBlacklistZookeeperCommand.class);

    private final String qualifiedTopicName;

    RemoveTopicFromBlacklistZookeeperCommand(String qualifiedTopicName, ZookeeperPaths paths) {
        super(paths);
        this.qualifiedTopicName = qualifiedTopicName;
    }

    @Override
    public void backup(ZookeeperClient client) {}

    @Override
    public void execute(ZookeeperClient client) {
        logger.info("Removing topic {} from Blacklist via client {}", qualifiedTopicName, client.getName());
        try {
            client.deleteWithChildrenWithGuarantee(paths.blacklistedTopicPath(qualifiedTopicName));
        } catch (Exception e) {
            logger.warn("Removing topic {} from Blacklist via client {} caused an exception", qualifiedTopicName,
                    client.getName(), e);
            throw new NotUnblacklistedException(qualifiedTopicName, e);
        }
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: topic {} blacklist removal via client {}", qualifiedTopicName,
                client.getName());
        client.ensurePathExists(paths.blacklistedTopicPath(qualifiedTopicName));
    }
}
