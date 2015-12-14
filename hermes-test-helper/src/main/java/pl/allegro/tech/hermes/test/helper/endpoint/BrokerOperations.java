package pl.allegro.tech.hermes.test.helper.endpoint;

import com.jayway.awaitility.Duration;
import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

/**
 * Created to perform operations directly on broker excluding Hermes internal structures
 */
public class BrokerOperations {

    private static final int DEFAULT_PARTITIONS = 2;
    private static final int DEFAULT_REPLICATION_FACTOR = 1;
    private List<ZkClient> zkClients;

    public BrokerOperations(List<String> kafkaZkConnection, int sessionTimeout, int connectionTimeout) {
        zkClients = kafkaZkConnection.stream().map(it -> new ZkClient(it, sessionTimeout, connectionTimeout, ZKStringSerializer$.MODULE$))
                .collect(Collectors.toList());
    }

    public void createTopic(String topicName) {
        zkClients.forEach(c -> {
            AdminUtils.createTopic(c, topicName, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR, new Properties());

            waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> {
                        AdminUtils.topicExists(c, topicName);
                    }
            );
        });
    }
}
