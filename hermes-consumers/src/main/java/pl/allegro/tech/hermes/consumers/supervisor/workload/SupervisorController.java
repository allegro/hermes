package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;

import java.util.Set;

public interface SupervisorController extends SubscriptionCallback, TopicCallback, SubscriptionAssignmentAware, AdminOperationsCallback {

    Set<SubscriptionName> assignedSubscriptions();

    void start() throws Exception;

    void shutdown() throws InterruptedException;

}
