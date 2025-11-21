package pl.allegro.tech.hermes.consumers.consumer.message

import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult

class MessageConverterTest extends Specification {
    def "should convert message to deadletter"() {
        given:
        def message = MessageBuilder.testMessage()
        def subscription = SubscriptionBuilder.subscription("pl.allegro.adam.topic", "subscription").build()
        def messageSendingResult = MessageSendingResult.failedResult(500)

        when:
        def deadMessage = MessageConverter.toDeadMessage(message, subscription, messageSendingResult)

        then:
        deadMessage.body == message.data
        deadMessage.messageId == message.id
        deadMessage.offset == message.offset
        deadMessage.partition == message.partition
        deadMessage.partitionAssignmentTerm == message.partitionAssignmentTerm
        deadMessage.topic == message.topic
        deadMessage.subscription == subscription.name
        deadMessage.kafkaTopic == message.kafkaTopic.asString()
        deadMessage.publishingTimestamp == message.publishingTimestamp
        deadMessage.readingTimestamp == message.readingTimestamp
        deadMessage.body == message.data
        deadMessage.rootCause == messageSendingResult.toString()
    }
}
