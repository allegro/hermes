package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.OAuthProviderPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class RemoveOAuthProviderZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(RemoveOAuthProviderZookeeperCommand.class);

    private final String providerName;
    private final OAuthProviderPreconditions preconditions;

    private byte[] providerDataBackup;

    RemoveOAuthProviderZookeeperCommand(String providerName, ZookeeperPaths paths) {
        super(paths);
        this.providerName = providerName;
        this.preconditions = new OAuthProviderPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        preconditions.ensureOAuthProviderExists(client, providerName);

        providerDataBackup = client.getData(getPath());
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureOAuthProviderExists(client, providerName);

        logger.info("Removing OAuthProvider {} via client {}", providerName, client.getName());
        client.deleteWithChildrenWithGuarantee(getPath());
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: OAuthProvider {} removal via client {}", providerName, client.getName());
        client.create(getPath(), providerDataBackup);
    }

    private String getPath() {
        return paths.oAuthProviderPath(providerName);
    }
}
