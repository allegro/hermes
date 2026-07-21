package pl.allegro.tech.hermes.frontend.publishing.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import pl.allegro.tech.hermes.api.ErrorCode
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

class TopicHandlerTest extends Specification {

    def "should report status and Hermes latency metrics before rejecting unauthorized publisher"() {
        given:
        Topic topic = TopicBuilder.topicWithRandomName()
                .withAuthEnabled()
                .withUnauthenticatedAccessDisabled()
                .build()
        CachedTopic cachedTopic = Mock()
        StartedTimersPair timers = Mock()
        TopicsCache topicsCache = Mock()
        MessageErrorProcessor errorProcessor = Mock()
        HttpHandler next = Mock()
        HttpServerExchange exchange = Mock()
        TopicHandler handler = new TopicHandler(next, topicsCache, errorProcessor)

        when:
        handler.handleRequest(exchange)

        then:
        1 * exchange.isInIoThread() >> false
        1 * exchange.getQueryParameters() >> [qualifiedTopicName: new ArrayDeque([topic.qualifiedName])]
        1 * topicsCache.getTopic(topic.qualifiedName) >> Optional.of(cachedTopic)
        1 * cachedTopic.getTopic() >> topic
        1 * cachedTopic.startHermesLatencyTimers() >> timers
        1 * exchange.addExchangeCompleteListener(_ as ExchangeMetrics)
        1 * exchange.getSecurityContext() >> null
        1 * errorProcessor.sendQuietly(
                exchange,
                { it.code == ErrorCode.AUTH_ERROR },
                _ as String,
                topic.qualifiedName)
        0 * next.handleRequest(_)
    }
}
