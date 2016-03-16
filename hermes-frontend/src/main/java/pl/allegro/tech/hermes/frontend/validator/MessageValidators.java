package pl.allegro.tech.hermes.frontend.validator;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import java.util.List;

public class MessageValidators {

    private final List<TopicMessageValidator> messageValidators;
    private final HermesMetrics hermesMetrics;

    @Inject
    public MessageValidators(List<TopicMessageValidator> messageValidators, HermesMetrics hermesMetrics) {
        this.messageValidators = messageValidators;
        this.hermesMetrics = hermesMetrics;
    }

    public void check(Topic topic, Message message) {
        try (Timer.Context globalValidationTimerContext = hermesMetrics.timer(Timers.VALIDATION_LATENCY).time();
             Timer.Context topicValidationTimerContext = hermesMetrics.timer(Timers.VALIDATION_LATENCY, topic.getName()).time()) {
            messageValidators.forEach(v -> v.check(message, topic));
        }
    }

}
