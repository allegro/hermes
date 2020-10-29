package pl.allegro.tech.hermes.frontend.publishing.handlers

import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import spock.lang.Specification


class DefaultResponseSimulatorTest extends Specification {

    def "should simulate default response generation"() {
        given:
        MessageReadHandler.DefaultResponseSimulator listener = new MessageReadHandler.DefaultResponseSimulator();
        HttpServerExchange exchange = new HttpServerExchange(null)
        AttachmentContent attachmentContent = new AttachmentContent(null, null, null)
        exchange.putAttachment(AttachmentContent.KEY, attachmentContent)

        expect:
        listener.handleDefaultResponse(exchange)
    }

    def "should not simulate default response generation for the first call when response was marked as ready"() {
        given:
        MessageReadHandler.DefaultResponseSimulator listener = new MessageReadHandler.DefaultResponseSimulator();
        HttpServerExchange exchange = new HttpServerExchange(null)
        exchange.getStatusCode() >> 201
        AttachmentContent attachmentContent = new AttachmentContent(null, null, null)
        exchange.putAttachment(AttachmentContent.KEY, attachmentContent)

        when:
        attachmentContent.markResponseAsReady()

        then:
        !listener.handleDefaultResponse(exchange)
        listener.handleDefaultResponse(exchange)
        listener.handleDefaultResponse(exchange)
    }

    def "should set response status 500 in case of unexpected status 200"() {
        given:
        MessageReadHandler.DefaultResponseSimulator listener = new MessageReadHandler.DefaultResponseSimulator();
        HttpServerExchange exchange = new HttpServerExchange(null)
        exchange.getStatusCode() >> 200
        exchange.setRequestMethod(HttpString.EMPTY)
        exchange.setRequestURI("")
        AttachmentContent attachmentContent = new AttachmentContent(null, null, null)
        attachmentContent.markResponseAsReady()
        exchange.putAttachment(AttachmentContent.KEY, attachmentContent)

        when:
        listener.handleDefaultResponse(exchange)

        then:
        exchange.getStatusCode() ==  500
    }
}
