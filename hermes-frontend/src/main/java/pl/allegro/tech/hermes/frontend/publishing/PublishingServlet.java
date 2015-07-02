package pl.allegro.tech.hermes.frontend.publishing;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.publishing.callbacks.BrokerListenersPublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.callbacks.HttpPublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.callbacks.MetricsPublishingCallback;
import pl.allegro.tech.hermes.frontend.validator.InvalidMessageException;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.strip;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_NOT_EXISTS;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

public class PublishingServlet extends HttpServlet {

    private final HermesMetrics hermesMetrics;
    private final ErrorSender errorSender;
    private final Trackers trackers;
    private final TopicsCache topicsCache;
    private final MessageValidators messageValidators;
    private final Clock clock;
    private final MessagePublisher messagePublisher;
    private final MessageContentTypeEnforcer contentTypeEnforcer;
    private final BrokerListeners listeners;

    private final Integer defaultAsyncTimeout;
    private final Integer longAsyncTimeout;
    private final Integer chunkSize;

    @Inject
    public PublishingServlet(TopicsCache topicsCache,
                             HermesMetrics hermesMetrics,
                             ObjectMapper objectMapper,
                             ConfigFactory configFactory,
                             Trackers trackers,
                             MessageValidators messageValidators,
                             Clock clock,
                             MessagePublisher messagePublisher,
                             BrokerListeners listeners) {

        this.topicsCache = topicsCache;
        this.messageValidators = messageValidators;
        this.clock = clock;
        this.messagePublisher = messagePublisher;
        this.contentTypeEnforcer = new MessageContentTypeEnforcer();
        this.errorSender = new ErrorSender(objectMapper);
        this.hermesMetrics = hermesMetrics;
        this.trackers = trackers;
        this.listeners = listeners;
        this.defaultAsyncTimeout = configFactory.getIntProperty(Configs.FRONTEND_IDLE_TIMEOUT);
        this.longAsyncTimeout = configFactory.getIntProperty(Configs.FRONTEND_LONG_IDLE_TIMEOUT);
        this.chunkSize = configFactory.getIntProperty(Configs.FRONTEND_REQUEST_CHUNK_SIZE);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TopicName topicName = parseTopicName(request);
        final String messageId = UUID.randomUUID().toString();
        Optional<Topic> topic = topicsCache.getTopic(topicName);

        if (topic.isPresent()) {
            handlePublishAsynchronously(request, response, topic.get(), messageId);
        } else {
            String cause = format("Topic %s not exists in group %s", topicName.getName(), topicName.getGroupName());
            errorSender.sendErrorResponse(new ErrorDescription(cause, TOPIC_NOT_EXISTS), response, messageId);
        }
    }

    private void handlePublishAsynchronously(HttpServletRequest request, HttpServletResponse response, Topic topic, String messageId)
            throws IOException {
        final MessageState messageState = new MessageState();
        final AsyncContext asyncContext = request.startAsync();
        final HttpResponder httpResponder = new HttpResponder(trackers, messageId, response, asyncContext, topic, errorSender, messageState,
                request.getRemoteHost());

        asyncContext.addListener(new TimeoutAsyncListener(httpResponder, messageState));
        asyncContext.addListener(new MetricsAsyncListener(hermesMetrics, topic.getName(), topic.getAck()));
        asyncContext.setTimeout(topic.isReplicationConfirmRequired() ? longAsyncTimeout : defaultAsyncTimeout);

        new MessageReader(request, chunkSize, topic.getName(), hermesMetrics, messageState,
                messageContent -> {
                    try {
                        Message message = contentTypeEnforcer.enforce(request.getContentType(),
                                new Message(messageId, messageContent, clock.getTime()), topic);

                        messageValidators.check(topic.getName(), message.getData());

                        asyncContext.addListener(new BrokerTimeoutAsyncListener(httpResponder, message, topic, messageState, listeners));

                        messagePublisher.publish(message, topic, messageState,
                                new HttpPublishingCallback(httpResponder),
                                new MetricsPublishingCallback(hermesMetrics, topic),
                                new BrokerListenersPublishingCallback(listeners));

                    } catch (InvalidMessageException exception) {
                        httpResponder.badRequest(exception);
                    }
                    return null;
                },
                input -> {
                    httpResponder.badRequest(input, "Validation error");
                    return null;
                },
                throwable -> {
                    httpResponder.internalError(throwable, "Error while reading request");
                    return null;
                });
    }

    private TopicName parseTopicName(HttpServletRequest request) {
        return fromQualifiedName(substringAfterLast(strip(request.getRequestURI(), "/"), "/"));
    }
}
