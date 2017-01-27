package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

public class SubscriptionAssignmentPathSerializer {

    private final String prefix;
    private final byte[] autoAssignedMarker;

    public SubscriptionAssignmentPathSerializer(String prefix, byte[] autoAssignedMarker) {
        this.prefix = prefix;
        this.autoAssignedMarker = Arrays.copyOf(autoAssignedMarker, autoAssignedMarker.length);
    }

    public String serialize(SubscriptionName subscriptionName, String supervisorId) {
        return Joiner.on("/").join(prefix, subscriptionName, supervisorId);
    }

    public String serialize(SubscriptionName subscriptionName) {
        return Joiner.on("/").join(prefix, subscriptionName);
    }

    public SubscriptionAssignment deserialize(String path, byte[] data) {
        String[] paths = path.split("/");
        checkArgument(paths.length > 1, "Incorrect path format. Expected:'/base/subscription/supervisorId'. Found:'%s'", path);
        boolean autoAssigned = data != null && Arrays.equals(data, autoAssignedMarker);
        return new SubscriptionAssignment(paths[paths.length - 1],
                SubscriptionName.fromString(paths[paths.length - 2]),
                autoAssigned);
    }
}
