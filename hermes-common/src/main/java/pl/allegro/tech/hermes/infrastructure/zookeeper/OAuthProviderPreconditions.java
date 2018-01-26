package pl.allegro.tech.hermes.infrastructure.zookeeper;

import pl.allegro.tech.hermes.domain.oauth.OAuthProviderNotExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;

public class OAuthProviderPreconditions {

    private final ZookeeperPaths paths;

    public OAuthProviderPreconditions(ZookeeperPaths paths) {
        this.paths = paths;
    }

    public void ensureOAuthProviderExists(ZookeeperClient client, String providerName) {
        if(!client.pathExists(paths.oAuthProviderPath(providerName))){
            throw new OAuthProviderNotExistsException(providerName);
        }
    }

}
