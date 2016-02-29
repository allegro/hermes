package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.integration.helper.schemarepo.SchemaRepo;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class SchemaRepoStarter implements Starter<SchemaRepo> {
    private final int port;
    private SchemaRepo repositoryServer;

    public SchemaRepoStarter(int port) {
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        repositoryServer = new SchemaRepo(port);
        repositoryServer.start();
    }

    @Override
    public void stop() throws Exception {
        repositoryServer.stop();
    }

    @Override
    public SchemaRepo instance() {
        return repositoryServer;
    }
}
