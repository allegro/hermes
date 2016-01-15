package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

public interface SupervisorController extends SubscriptionCallback, SubscriptionAssignmentAware, AdminOperationsCallback {
    void start() throws Exception;
    void shutdown() throws InterruptedException;
}
