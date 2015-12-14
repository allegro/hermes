package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.SubscriptionName;

import static com.google.common.base.Preconditions.checkArgument;

public class SubscriptionAssignmentPathSerializer {
    private final String prefix;

    public SubscriptionAssignmentPathSerializer(String prefix) {
        this.prefix = prefix;
    }

    public String serialize(SubscriptionName subscriptionName, String supervisorId) {
        return Joiner.on("/").join(prefix, subscriptionName, supervisorId);
    }

    public SubscriptionAssignment deserialize(String path) {
        String[] paths = path.split("/");
        checkArgument(paths.length > 1, "Incorrect path format. Expected:'/base/subscription/supervisorId'. Found:'%s'", path);
        return new SubscriptionAssignment(paths[paths.length - 1], SubscriptionName.fromString(paths[paths.length - 2]));
    }
}
