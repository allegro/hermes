package pl.allegro.tech.hermes.management.infrastructure.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.topic.blacklist.BlacklistRepository;
import pl.allegro.tech.hermes.management.domain.topic.blacklist.NotBlacklistedException;

public class ZookeeperBlacklistRepository extends ZookeeperBasedRepository implements BlacklistRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperBlacklistRepository.class);

    public ZookeeperBlacklistRepository(CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
        super(zookeeper, mapper, paths);
    }

    @Override
    public void add(String qualifiedTopicName) {
        logger.info("Adding topic {} to Blacklist", qualifiedTopicName);
        ensurePathExists(paths.blacklistedTopicPath(qualifiedTopicName));
    }

    @Override
    public void remove(String qualifiedTopicName) {
        logger.info("Removing topic {} from Blacklist", qualifiedTopicName);
        ensureBlacklisted(qualifiedTopicName);
        super.remove(paths.blacklistedTopicPath(qualifiedTopicName));
    }

    @Override
    public boolean isBlacklisted(String qualifiedTopicName) {
        return pathExists(paths.blacklistedTopicPath(qualifiedTopicName));
    }

    private void ensureBlacklisted(String qualifiedTopicName) {
        if (!isBlacklisted(qualifiedTopicName)) {
            throw new NotBlacklistedException(qualifiedTopicName);
        }
    }
}
