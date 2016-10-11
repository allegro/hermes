package pl.allegro.tech.hermes.consumers.consumer.filtering

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.common.filtering.FilteringException
import pl.allegro.tech.hermes.common.filtering.avro.AvroPathSubscriptionMessageFilterCompiler
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import spock.lang.Specification
import spock.lang.Unroll
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

class AvroPathMessageFilterSpec extends Specification {

    @Unroll
    def "basic paths"(String path, String matcher, boolean result) {
        given:
        def schema = AvroUserSchemaLoader.load("/cake.avsc")

        def json = '''
            {
                "id": "0001",
                "type": "donut",
                "name": "Cake",
                "ppu": 0.55,
                "batter": {
                     "id": "1003",
                     "type": "Blueberry"
                },
                "topping": {
                     "id": "5004",
                     "type": "Maple",
                     "description": "Maple syrup"
                }
            }
        '''

        def avro = new JsonAvroConverter().convertToAvro(json.bytes, schema)
        def spec = new MessageFilterSpecification([path: path, matcher: matcher])
        def msg = MessageBuilder
                .withTestMessage()
                .withContent(avro)
                .withSchema(schema, 0)
                .withContentType(ContentType.AVRO)
                .build()

        expect:
        result == new AvroPathSubscriptionMessageFilterCompiler().compile(spec).test(msg)

        where:
        path                      | matcher     | result
        ".id"                     | "0001"      | true
        ".does.not.exist"         | ".*"        | false
        ".id"                     | "000.?"     | true
        ".id"                     | "0002"      | false
        ".type"                   | "donut"     | true
        ".type"                   | "not_donut" | false
        ".batter.id"              | "1003"      | true
        ".batter.id"              | "1004"      | false
        ".topping.type"           | "^Map.*"    | true
        ".topping.description"    | ".*syrup.*" | true
        ".topping.description.a"  | ".*"        | false
    }

    def "should throw exception for malformed message"() {
        given:
        def schema = AvroUserSchemaLoader.load("/cake.avsc")
        def invalidContent = new byte[10]

        when:
        new AvroPathSubscriptionMessageFilterCompiler().compile(new MessageFilterSpecification([path: ".id", matcher: "0001"]))
                .test(MessageBuilder
                .withTestMessage()
                .withContent(invalidContent)
                .withSchema(schema, 0)
                .build())

        then:
        thrown FilteringException
    }
}


