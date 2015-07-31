package pl.allegro.tech.hermes.frontend.validator;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicCallback;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MessageValidators implements TopicCallback {

    private static final Logger logger = LoggerFactory.getLogger(MessageValidators.class);

    private final Map<TopicName, TopicMessageValidator> topicsWithValidators;
    private final TopicMessageValidatorFactory topicMessageValidatorFactory;
    private final HermesMetrics hermesMetrics;

    @Inject
    public MessageValidators(TopicMessageValidatorFactory topicMessageValidatorFactory, HermesMetrics hermesMetrics) {
        this.topicMessageValidatorFactory = topicMessageValidatorFactory;
        this.hermesMetrics = hermesMetrics;
        topicsWithValidators = new ConcurrentHashMap<>();
    }

    @Override
    public void onTopicCreated(Topic topic) {
        if (topic.isValidationEnabled()) {
            addValidation(topic);
        }
    }

    @Override
    public void onTopicRemoved(Topic topic) {
        logIfValidationRemoved(topicsWithValidators.remove(topic.getName()), topic.getQualifiedName());
    }

    @Override
    public void onTopicChanged(Topic topic) {
        if (topic.isValidationEnabled()) {
            addValidation(topic);
        } else {
            logIfValidationRemoved(topicsWithValidators.remove(topic.getName()), topic.getQualifiedName());
        }
    }

    public void check(Topic topic, byte[] message) {
        Optional.ofNullable(topicsWithValidators.get(topic.getName())).ifPresent(validator -> {
            try (Timer.Context globalValidationTimerContext = hermesMetrics.timer(Timers.PRODUCER_VALIDATION_LATENCY).time();
                 Timer.Context topicValidationTimerContext = hermesMetrics.timer(Timers.PRODUCER_VALIDATION_LATENCY, topic.getName()).time()) {
                validator.check(message, topic);
            }
        });
    }

    private void addValidation(Topic topic) {
        try {
            topicsWithValidators.put(topic.getName(), topicMessageValidatorFactory.create(topic));
            logger.info("Enabled validation for topic {}", topic.getQualifiedName());
        } catch (Exception e) {
            logger.error("Error while creating message validator for topic: " + topic.getName(), e);
        }
    }

    private void logIfValidationRemoved(TopicMessageValidator previousTopicValidator, String topicQualifiedName) {
        if (previousTopicValidator != null) {
            logger.info("Disabled validation for topic: {}", topicQualifiedName);
        }
    }
}
