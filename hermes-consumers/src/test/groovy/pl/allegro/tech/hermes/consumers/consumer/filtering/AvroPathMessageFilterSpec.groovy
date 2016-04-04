package pl.allegro.tech.hermes.consumers.consumer.filtering

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.consumers.consumer.filtering.avro.AvroPathSubscriptionMessageFilterCompiler
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader
import spock.lang.Specification
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

class AvroPathMessageFilterSpec extends Specification {

    def "basic paths"(String path, String matcher, boolean result) {
        given:
        def schema = AvroUserSchemaLoader.load("/cake.avsc")

        def json = '''
            {
                "id": "0001",
                "type": "donut",
                "name": "Cake",
                "ppu": 0.55,
                "batters":
                    {
                        "batter":
                            [
                                { "id": "1001", "type": "Regular" },
                                { "id": "1002", "type": "Chocolate" },
                                { "id": "1003", "type": "Blueberry" },
                                { "id": "1004", "type": "Devil's Food" }
                            ]
                    },
                "topping":
                    [
                        { "id": "5001", "type": "None" },
                        { "id": "5002", "type": "Glazed" },
                        { "id": "5005", "type": "Sugar" },
                        { "id": "5007", "type": "Powdered Sugar" },
                        { "id": "5006", "type": "Chocolate with Sprinkles" },
                        { "id": "5003", "type": "Chocolate" },
                        { "id": "5004", "type": "Maple" }
                    ]
            }
        '''

        def avro = new JsonAvroConverter().convertToAvro(json.bytes, schema)
        def spec = new MessageFilterSpecification([path: path, matcher: matcher])

        expect:
        result == new AvroPathSubscriptionMessageFilterCompiler().compile(spec)
                .test(MessageBuilder
                .withTestMessage()
                .withContent(avro)
                .withSchema(schema, 0)
                .withContentType(ContentType.AVRO)
                .build())

        where:
        path                      | matcher     | result
        ".id"                     | "0001"      | true
        ".does.not.exist"         | ".*"        | false
        ".id"                     | "000.?"     | true
        ".id"                     | "0002"      | false
        '.type'                   | "donut"     | true
        '.type'                   | "not_donut" | false
        '.batters.batter[1].type' | "^Choco.*"  | true
        '.batters.batter[2].type' | "^Choco.*"  | false
        '.{.ppu > 0.5}.name'      | "Cake"      | true
        '.{.ppu < 0.5}.name'      | "Cake"      | false
        '.topping[4:5].type'      | "^Choco.*"  | true
        '.topping[4:6].type'      | "^Choco.*"  | false
        //'.topping{.id === "5007"}.type' | "^Powdered.*" | true  BUG in avpath. Master is ok, 0.1.0 from maven central has a bug.
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


