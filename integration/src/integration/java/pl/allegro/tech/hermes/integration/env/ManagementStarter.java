package pl.allegro.tech.hermes.integration.env;

import org.apache.commons.lang.ArrayUtils;
import pl.allegro.tech.hermes.management.HermesManagement;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class ManagementStarter implements Starter<HermesManagement> {

    private final int port;
    private final String env;

    private final String[] additionalArgs;

    public ManagementStarter(int port, String env) {
        this(port, env, new String[]{});
    }

    public ManagementStarter(int port, String env, String[] additionalArgs) {
        this.port = port;
        this.env = env;
        this.additionalArgs = additionalArgs;
    }

    @Override
    public void start() throws Exception {
        String[] mergedArgs = (String[]) ArrayUtils.addAll(new String[]{"-p", "" + port, "-e", env}, additionalArgs);
        HermesManagement.main(mergedArgs);
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public HermesManagement instance() {
        return null;
    }
}
