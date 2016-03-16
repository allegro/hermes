package pl.allegro.tech.hermes.integration.env;

import org.schemarepo.config.Config;
import org.schemarepo.server.RepositoryServer;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.Properties;

public class SchemaRepoStarter implements Starter<RepositoryServer> {
    private final int port;
    private RepositoryServer repositoryServer;

    public SchemaRepoStarter(int port) {
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        Properties properties = new Properties();
        properties.put(Config.REPO_CLASS, "org.schemarepo.InMemoryRepository");
        properties.put(Config.JETTY_PORT, port);
        repositoryServer = new RepositoryServer(properties);
        repositoryServer.start();
    }

    @Override
    public void stop() throws Exception {
        repositoryServer.stop();
    }

    @Override
    public RepositoryServer instance() {
        return repositoryServer;
    }
}
