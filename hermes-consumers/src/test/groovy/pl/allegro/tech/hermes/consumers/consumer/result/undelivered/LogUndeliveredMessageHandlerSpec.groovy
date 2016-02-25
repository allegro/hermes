package pl.allegro.tech.hermes.consumers.consumer.result.undelivered

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import spock.lang.Specification

import java.time.Clock

class LogUndeliveredMessageHandlerSpec extends Specification {

    def "should log message to undeliveredMessageLog"() {
        given:
        UndeliveredMessageLog undeliveredMessageLog = Mock()

        LogUndeliveredMessageHandler logUndeliveredMessageHandler = new LogUndeliveredMessageHandler(undeliveredMessageLog, Stub(Clock), Stub(ConfigFactory))

        when:
        logUndeliveredMessageHandler.handleDiscarded(Stub(Message), Stub(Subscription), Stub(MessageSendingResult))

        then:
        1 * undeliveredMessageLog.add(_)
    }
}