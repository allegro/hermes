package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderAlreadyExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class CreateOAuthProviderZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateOAuthProviderZookeeperCommand.class);

    private final OAuthProvider provider;
    private final ObjectMapper mapper;

    CreateOAuthProviderZookeeperCommand(OAuthProvider provider, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.provider = provider;
        this.mapper = mapper;
    }

    @Override
    public void backup(ZookeeperClient client) {}

    @Override
    public void execute(ZookeeperClient client) {
        String path = paths.oAuthProviderPath(provider.getName());
        logger.info("Creating OAuthProvider for path {} via client {}", path, client.getName());

        try {
            client.getCuratorFramework().create().creatingParentsIfNeeded().forPath(path, marshall(mapper, provider));
        } catch (KeeperException.NodeExistsException e) {
            throw new OAuthProviderAlreadyExistsException(provider, e);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void rollback(ZookeeperClient client) {
        client.deleteWithChildren(paths.oAuthProviderPath(provider.getName()));
    }
}
