package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;

public interface SupervisorController extends SubscriptionCallback, TopicCallback, SubscriptionAssignmentAware, AdminOperationsCallback {

    void start() throws Exception;

    void shutdown() throws InterruptedException;
}
