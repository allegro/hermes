package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderAlreadyExistsException;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderNotExistsException;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class ZookeeperOAuthProviderRepository extends ZookeeperBasedRepository implements OAuthProviderRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperOAuthProviderRepository.class);

    public ZookeeperOAuthProviderRepository(CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
        super(zookeeper, mapper, paths);
    }

    @Override
    public List<String> listOAuthProviderNames() {
        ensurePathExists(paths.oAuthProvidersPath());
        return childrenOf(paths.oAuthProvidersPath());
    }

    @Override
    public List<OAuthProvider> listOAuthProviders() {
        ensurePathExists(paths.oAuthProvidersPath());
        return listOAuthProviderNames().stream()
                .map(this::getOAuthProviderDetails)
                .collect(toList());
    }

    @Override
    public OAuthProvider getOAuthProviderDetails(String oAuthProviderName) {
        ensureOAuthProviderExists(oAuthProviderName);
        return readFrom(paths.oAuthProviderPath(oAuthProviderName), OAuthProvider.class);
    }

    @Override
    public void createOAuthProvider(OAuthProvider oAuthProvider) {
        ensureConnected();

        String oAuthProviderPath = paths.oAuthProviderPath(oAuthProvider.getName());
        logger.info("Creating OAuthProvider for path {}", oAuthProviderPath);

        try {
            zookeeper.create().creatingParentsIfNeeded().forPath(oAuthProviderPath, mapper.writeValueAsBytes(oAuthProvider));
        } catch (KeeperException.NodeExistsException ex) {
            throw new OAuthProviderAlreadyExistsException(oAuthProvider, ex);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void updateOAuthProvider(OAuthProvider oAuthprovider) {
        ensureOAuthProviderExists(oAuthprovider.getName());

        logger.info("Updating OAuthProvider {}", oAuthprovider.getName());
        overwrite(paths.oAuthProviderPath(oAuthprovider.getName()), oAuthprovider);
    }

    @Override
    public void removeOAuthProvider(String oAuthProviderName) {
        ensureOAuthProviderExists(oAuthProviderName);

        logger.info("Removing OAuthProvider {}", oAuthProviderName);
        remove(paths.oAuthProviderPath(oAuthProviderName));
    }

    @Override
    public void ensureOAuthProviderExists(String oAuthProviderName) {
        if (!oAuthProviderExists(oAuthProviderName)) {
            throw new OAuthProviderNotExistsException(oAuthProviderName);
        }
    }

    @Override
    public boolean oAuthProviderExists(String oAuthProviderName) {
        return pathExists(paths.oAuthProviderPath(oAuthProviderName));
    }
}
