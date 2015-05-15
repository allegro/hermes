package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.management.HermesManagement;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class ManagementStarter implements Starter<HermesManagement> {

    private final int port;

    public ManagementStarter(int port) {
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        HermesManagement.main(new String[]{"-p", "" + port, "-e", "integration"});
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public HermesManagement instance() {
        return null;
    }
}
