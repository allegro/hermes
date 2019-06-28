package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RetransmitCommand extends RepositoryCommand<AdminTool> {

    private final SubscriptionName subscriptionName;

    public RetransmitCommand(SubscriptionName subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    public void backup(AdminTool repository) {
        repository.start();
    }

    @Override
    public void execute(AdminTool repository) {
        repository.retransmit(subscriptionName);
    }

    @Override
    public void rollback(AdminTool repository) {

    }

    @Override
    public Class getRepositoryType() {
        return AdminTool.class;
    }
}
