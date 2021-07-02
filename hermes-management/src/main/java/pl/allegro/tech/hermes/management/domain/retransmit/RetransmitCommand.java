package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RetransmitCommand extends RepositoryCommand<AdminTool> {

    private final SubscriptionName subscriptionName;

    public RetransmitCommand(SubscriptionName subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<AdminTool> holder) {
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<AdminTool> holder) {
        holder.getRepository().retransmit(subscriptionName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<AdminTool> holder) {

    }

    @Override
    public Class<AdminTool> getRepositoryType() {
        return AdminTool.class;
    }
}
