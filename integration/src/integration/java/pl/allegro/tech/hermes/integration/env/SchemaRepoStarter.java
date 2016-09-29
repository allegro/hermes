package pl.allegro.tech.hermes.integration.env;

import org.glassfish.jersey.internal.ServiceFinder;
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

        // temporarily overriding Jersey's default ServiceIteratorProvider
        // in order to filter out SpringComponentProvider (available in classpath)
        // which tries to initialize custom application context from an XML file
        ServiceFinder.setIteratorProvider(SpringlessServiceIteratorProvider.INSTANCE);

        Properties properties = new Properties();
        properties.put(Config.REPO_CLASS, "org.schemarepo.InMemoryRepository");
        properties.put(Config.JETTY_PORT, port);
        repositoryServer = new RepositoryServer(properties);
        repositoryServer.start();

        ServiceFinder.setIteratorProvider(null);
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
