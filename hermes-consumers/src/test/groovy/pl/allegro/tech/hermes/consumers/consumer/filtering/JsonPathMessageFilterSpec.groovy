package pl.allegro.tech.hermes.consumers.consumer.filtering

import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.consumers.consumer.filtering.json.JsonPathSubscriptionMessageFilterCompiler
import spock.lang.Specification

import static java.nio.charset.Charset.defaultCharset
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage

class JsonPathMessageFilterSpec extends Specification {

    def "basic paths"(String path, String matcher, boolean result) {
        given:
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

        def spec = new MessageFilterSpecification([path: path, matcher: matcher])

        expect:
        result == new JsonPathSubscriptionMessageFilterCompiler().compile(spec)
                .test(withTestMessage()
                    .withContent(json, defaultCharset())
                    .build())

        where:
        path                                  | matcher       | result
        '$.type'                              | "donut"       | true
        '$.type'                              | "not_donut"   | false
        '$.does.not.exist'                    | ".*"          | false
        '$.batters.batter[1].type'            | "^Choco.*"    | true
        '$.batters.batter[2].type'            | "^Choco.*"    | false
        '$.[?(@.ppu > 0.5)].name'             | "Cake"        | true
        '$.[?(@.ppu < 0.5)].name'             | "Cake"        | false
        '$.topping[?(@.id == 5007)].type'     | "^Powdered.*" | true
        '$.topping[?(@.id == 5007)].type'     | "^Maple.*"    | false
        '$.topping[?(@.id == 5001)].type'     | "None"        | true
        '$.topping[4:6].type'                 | "^Choco.*"    | true
        '$.topping[4:7].type'                 | "^Choco.*"    | false
    }
}
