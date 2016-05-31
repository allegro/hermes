package pl.allegro.tech.hermes.consumers.supervisor.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.common.exception.RetransmissionException;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;

import java.util.List;

import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class Retransmitter {

    private static final Logger logger = LoggerFactory.getLogger(Retransmitter.class);

    private SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;
    private List<OffsetsStorage> offsetsStorages;
    private String brokersClusterName;

    public Retransmitter(SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                         List<OffsetsStorage> offsetsStorages,
                         ConfigFactory configs) {
        this.subscriptionOffsetChangeIndicator = subscriptionOffsetChangeIndicator;
        this.offsetsStorages = offsetsStorages;
        this.brokersClusterName = configs.getStringProperty(KAFKA_CLUSTER_NAME);
    }

    public void reloadOffsets(SubscriptionName subscriptionName) {
        try {
            PartitionOffsets offsets = subscriptionOffsetChangeIndicator.getSubscriptionOffsets(
                    subscriptionName.getTopicName(), subscriptionName.getName(), brokersClusterName);

            for (PartitionOffset partitionOffset : offsets) {
                for (OffsetsStorage s: offsetsStorages) {
                    s.setSubscriptionOffset(subscriptionName, partitionOffset);
                }
            }
        } catch (Exception ex) {
            throw new RetransmissionException(ex);
        }
    }
}
