package pl.allegro.tech.hermes.frontend.server

import io.undertow.io.Sender
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.StatusCodes
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService
import pl.allegro.tech.hermes.frontend.readiness.ReadinessChecker
import spock.lang.Specification

class PublishingReadinessHandlerTest extends Specification {

    def "should pass request to next handler when frontend is ready"() {
        given:
        HttpHandler next = Mock()
        ReadinessChecker readinessChecker = Mock()
        HealthCheckService healthCheckService = Mock()
        HttpServerExchange exchange = Mock()
        PublishingReadinessHandler handler = new PublishingReadinessHandler(next, readinessChecker, healthCheckService)

        when:
        handler.handleRequest(exchange)

        then:
        1 * healthCheckService.isShutdown() >> false
        1 * readinessChecker.isReady() >> true
        1 * next.handleRequest(exchange)
        0 * exchange.setStatusCode(_)
        0 * exchange.getResponseSender()
    }

    def "should reject request when readiness checker is not ready"() {
        given:
        HttpHandler next = Mock()
        ReadinessChecker readinessChecker = Mock()
        HealthCheckService healthCheckService = Mock()
        HttpServerExchange exchange = Mock()
        Sender sender = Mock()
        PublishingReadinessHandler handler = new PublishingReadinessHandler(next, readinessChecker, healthCheckService)

        when:
        handler.handleRequest(exchange)

        then:
        1 * healthCheckService.isShutdown() >> false
        1 * readinessChecker.isReady() >> false
        1 * exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE)
        1 * exchange.getResponseSender() >> sender
        1 * sender.send("NOT_READY")
        0 * next.handleRequest(_)
    }

    def "should reject request when frontend is in shutdown mode"() {
        given:
        HttpHandler next = Mock()
        ReadinessChecker readinessChecker = Mock()
        HealthCheckService healthCheckService = Mock()
        HttpServerExchange exchange = Mock()
        Sender sender = Mock()
        PublishingReadinessHandler handler = new PublishingReadinessHandler(next, readinessChecker, healthCheckService)

        when:
        handler.handleRequest(exchange)

        then:
        1 * healthCheckService.isShutdown() >> true
        0 * readinessChecker.isReady()
        1 * exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE)
        1 * exchange.getResponseSender() >> sender
        1 * sender.send("NOT_READY")
        0 * next.handleRequest(_)
    }
}
