package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.OAuthProviderPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class UpdateOAuthProviderZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateOAuthProviderZookeeperCommand.class);

    private final OAuthProvider provider;
    private final OAuthProviderPreconditions preconditions;
    private final ObjectMapper mapper;

    private byte[] providerDataBackup;

    UpdateOAuthProviderZookeeperCommand(OAuthProvider provider, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.provider = provider;
        this.preconditions = new OAuthProviderPreconditions(paths);
        this.mapper = mapper;
    }

    @Override
    public void backup(ZookeeperClient client) {
        preconditions.ensureOAuthProviderExists(client, provider.getName());

        providerDataBackup = client.getData(getPath());
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureOAuthProviderExists(client, provider.getName());

        logger.info("Updating OAuthProvider '{}' via client '{}'", provider.getName(), client.getName());
        client.setData(getPath(), marshall(mapper, provider));
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: OAuthProvider '{}' update via client '{}'", provider.getName(),
                client.getName());
        client.setData(getPath(), providerDataBackup);
    }

    private String getPath() {
        return paths.oAuthProviderPath(provider.getName());
    }
}
