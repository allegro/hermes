package pl.allegro.tech.hermes.management.domain.credentials.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.domain.NodePassword;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperCredentialsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.dc.MultiDcRepositoryCommandExecutor;

@Component
public class CredentialsService {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsService.class);

    private final ZookeeperPaths paths;
    private final MultiDcRepositoryCommandExecutor multiDcExecutor;
    private final ZookeeperCredentialsRepository zookeeperCredentialsRepository;

    @Autowired
    public CredentialsService(ZookeeperPaths paths,
                              MultiDcRepositoryCommandExecutor multiDcExecutor,
                              ZookeeperCredentialsRepository zookeeperCredentialsRepository) {
        this.paths = paths;
        this.multiDcExecutor = multiDcExecutor;
        this.zookeeperCredentialsRepository = zookeeperCredentialsRepository;
    }

    public NodePassword readAdminPassword() {
        return zookeeperCredentialsRepository.readAdminPassword();
    }

    public void overwriteAdminPassword(String password) {
        multiDcExecutor.execute(new UpdateCredentialsRepositoryCommand(zookeeperCredentialsRepository, password));
    }
}
