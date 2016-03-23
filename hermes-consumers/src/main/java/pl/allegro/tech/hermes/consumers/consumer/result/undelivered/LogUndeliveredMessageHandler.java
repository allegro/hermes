package pl.allegro.tech.hermes.consumers.consumer.result.undelivered;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import javax.inject.Inject;
import java.time.Clock;

import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class LogUndeliveredMessageHandler implements UndeliveredMessageHandler {

    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final String cluster;

    @Inject
    public LogUndeliveredMessageHandler(UndeliveredMessageLog undeliveredMessageLog, Clock clock, ConfigFactory configFactory) {
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.clock = clock;
        this.cluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
    }

    @Override
    public void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result) {
        undeliveredMessageLog.add(createUndeliveredMessage(subscription, new String(message.getData()), result.getFailure(), clock.millis(),
                message.getPartition(), message.getOffset(), cluster));

    }
}
