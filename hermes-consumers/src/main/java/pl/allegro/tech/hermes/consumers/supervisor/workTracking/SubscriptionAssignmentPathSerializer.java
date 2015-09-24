package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import static com.google.common.base.Preconditions.checkArgument;

public class SubscriptionAssignmentPathSerializer {
    private final String prefix;
    private final String supervisorId;

    public SubscriptionAssignmentPathSerializer(String prefix, String supervisorId) {
        this.prefix = prefix;
        this.supervisorId = supervisorId;
    }

    public String serialize(Subscription subscription) {
        return Joiner.on("/").join(prefix, subscription.toSubscriptionName(), supervisorId);
    }

    public SubscriptionAssignment deserialize(String path) {
        String[] paths = path.split("/");
        checkArgument(paths.length > 1, "Incorrect path format. Expected:'/base/subscription/supervisorId'. Found:'%s'", path);
        return new SubscriptionAssignment(paths[paths.length - 1], SubscriptionName.fromString(paths[paths.length - 2]));
    }
}
