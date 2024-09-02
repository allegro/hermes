package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json

import org.json.JSONException
import org.json.JSONObject
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification

class GoogleBigQueryJsonMessageTransformerTest extends Specification {
    def 'should translate valid message to json'() {
        given:
        GoogleBigQueryJsonMessageTransformer transformer = new GoogleBigQueryJsonMessageTransformer()
        byte[] content = "{'message': 'some text'}".bytes
        String expected = "{\"message\":\"some text\"}"
        Message message = MessageBuilder.withTestMessage()
                .withContentType(ContentType.JSON)
                .withContent(content)
                .build()

        when:
        JSONObject object = transformer.fromHermesMessage(message)

        then:
        object.toString() == expected
    }

    def 'should throw an exception when json is invalid'() {
        given:
        GoogleBigQueryJsonMessageTransformer transformer = new GoogleBigQueryJsonMessageTransformer()
        byte[] content = "not a json".bytes
        Message message = MessageBuilder.withTestMessage()
                .withContentType(ContentType.JSON)
                .withContent(content)
                .build()

        when:
        transformer.fromHermesMessage(message)

        then:
        thrown(JSONException)
    }
    def 'should throw an exception when message is not a json'() {
        given:
        GoogleBigQueryJsonMessageTransformer transformer = new GoogleBigQueryJsonMessageTransformer()
        byte[] content = "not a json".bytes
        Message message = MessageBuilder.withTestMessage()
                .withContentType(ContentType.AVRO)
                .withContent(content)
                .build()

        when:
        transformer.fromHermesMessage(message)

        then:
        thrown(IllegalArgumentException)
    }
}

