package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.base.Preconditions;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResult;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingException;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingObserver;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesBatchProducingResults;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class KafkaMessagesProducingObserver implements BrokerMessagesProducingObserver {

    private final Collection<BrokerMessagesProducingResult> validationResults;
    private final CountDownLatch countDownLatch;
    private final long timeoutMs;

    public KafkaMessagesProducingObserver(int messagesCounter, long timeoutMs) {
        Preconditions.checkArgument(messagesCounter > 0, "Must observe positive number of messages to be produced");
        Preconditions.checkArgument(timeoutMs > 0L, "Timeout must be defined");
        this.countDownLatch = new CountDownLatch(messagesCounter);
        validationResults = new ArrayBlockingQueue<>(messagesCounter);
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void notifyAboutBrokerMessageProducingResult(BrokerMessagesProducingResult brokerMessagesProducingResult) {
        validationResults.add(brokerMessagesProducingResult);
        countDownLatch.countDown();
    }

    @Override
    public BrokerMessagesBatchProducingResults waitForMessagesBatchProducingResults() {
        try {
            boolean completed = countDownLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (completed) {
                return new BrokerMessagesBatchProducingResults(validationResults);
            } else {
                throw new BrokerMessagesProducingException("Timeout while publishing messages");
            }
        } catch (InterruptedException e) {
            throw new BrokerMessagesProducingException("Error while publishing messages", e);
        }
    }
}
