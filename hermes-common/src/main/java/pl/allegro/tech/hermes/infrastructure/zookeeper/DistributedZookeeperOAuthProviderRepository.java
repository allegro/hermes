package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;

import java.util.List;
import java.util.stream.Collectors;

public class DistributedZookeeperOAuthProviderRepository extends DistributedZookeeperRepository
        implements OAuthProviderRepository {
    private final ZookeeperCommandExecutor commandExecutor;
    private final ZookeeperCommandFactory commandFactory;
    private final ZookeeperPaths paths;
    private final OAuthProviderPreconditions preconditions;

    public DistributedZookeeperOAuthProviderRepository(ZookeeperClientManager clientManager,
                                                       ZookeeperCommandExecutor commandExecutor,
                                                       ZookeeperCommandFactory commandFactory,
                                                       ZookeeperPaths paths,
                                                       ObjectMapper mapper) {
        super(clientManager, mapper);
        this.commandExecutor = commandExecutor;
        this.commandFactory = commandFactory;
        this.paths = paths;
        this.preconditions = new OAuthProviderPreconditions(paths);
    }

    @Override
    public boolean oAuthProviderExists(String providerName) {
        ZookeeperClient client = clientManager.getLocalClient();
        String path = paths.oAuthProviderPath(providerName);
        return client.pathExists(path);
    }

    @Override
    public void ensureOAuthProviderExists(String oAuthProviderName) {
        ZookeeperClient client = clientManager.getLocalClient();
        preconditions.ensureOAuthProviderExists(client, oAuthProviderName);
    }

    @Override
    public List<String> listOAuthProviderNames() {
        ZookeeperClient client = clientManager.getLocalClient();
        return listOAuthProviderNames(client);
    }

    private List<String> listOAuthProviderNames(ZookeeperClient client) {
        return client.childrenOf(paths.oAuthProvidersPath());
    }

    @Override
    public List<OAuthProvider> listOAuthProviders() {
        ZookeeperClient client = clientManager.getLocalClient();
        return listOAuthProviderNames(client)
                .stream()
                .map(name -> getOAuthProviderDetails(client, name))
                .collect(Collectors.toList());
    }

    @Override
    public OAuthProvider getOAuthProviderDetails(String providerName) {
        ZookeeperClient client = clientManager.getLocalClient();
        return getOAuthProviderDetails(client, providerName);
    }

    private OAuthProvider getOAuthProviderDetails(ZookeeperClient client, String providerName) {
        preconditions.ensureOAuthProviderExists(client, providerName);
        byte[] data = client.getData(paths.oAuthProviderPath(providerName));
        return unmarshall(data, OAuthProvider.class);
    }

    @Override
    public void createOAuthProvider(OAuthProvider provider) {
        ZookeeperCommand command = commandFactory.createOAuthProvider(provider);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void updateOAuthProvider(OAuthProvider provider) {
        ZookeeperCommand command = commandFactory.updateOAuthProvider(provider);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void removeOAuthProvider(String providerName) {
        ZookeeperCommand command = commandFactory.removeOAuthProvider(providerName);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }
}
