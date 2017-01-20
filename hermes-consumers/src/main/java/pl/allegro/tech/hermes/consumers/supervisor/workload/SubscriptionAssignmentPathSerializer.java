package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

public class SubscriptionAssignmentPathSerializer {

    private final String prefix;
    private final byte[] autoMarker;

    public SubscriptionAssignmentPathSerializer(String prefix, byte[] autoMarker) {
        this.prefix = prefix;
        this.autoMarker = autoMarker;
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
        boolean auto = data != null && Arrays.equals(data, autoMarker);
        return new SubscriptionAssignment(paths[paths.length - 1],
                SubscriptionName.fromString(paths[paths.length - 2]),
                auto);
    }
}
