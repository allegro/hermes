package pl.allegro.tech.hermes.consumers.consumer.filtering

import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.consumers.consumer.filtering.json.JsonPathSubscriptionMessageFilterCompiler
import spock.lang.Specification
import spock.lang.Unroll

import static java.nio.charset.Charset.defaultCharset
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage

class JsonPathMessageFilterSpec extends Specification {

    @Unroll
    def "should filter '#path' matching '#matcher' with result: #result"(String path, String matcher, String matchingStrategy, boolean result) {
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
                    ],
                "size": ["small", "medium", "large"]
            }
        '''

        def spec = new MessageFilterSpecification([path: path, matcher: matcher, matchingStrategy: matchingStrategy])

        expect:
        result == new JsonPathSubscriptionMessageFilterCompiler().compile(spec)
                .test(withTestMessage()
                .withContent(json, defaultCharset())
                .build())

        where:
        path                              | matcher       | matchingStrategy | result
        '$.type'                          | "donut"       | null             | true
        '$.type'                          | "donut"       | "all"            | true
        '$.type'                          | "donut"       | "any"            | true
        '$.type'                          | "not_donut"   | null             | false
        '$.type'                          | "not_donut"   | "all"            | false
        '$.type'                          | "not_donut"   | "any"            | false
        '$.does.not.exist'                | ".*"          | null             | false
        '$.does.not.exist'                | ".*"          | "all"            | false
        '$.does.not.exist'                | ".*"          | "any"            | false
        '$.batters.batter[1].type'        | "^Choco.*"    | null             | true
        '$.batters.batter[1].type'        | "^Choco.*"    | "all"            | true
        '$.batters.batter[1].type'        | "^Choco.*"    | "any"            | true
        '$.batters.batter[2].type'        | "^Choco.*"    | null             | false
        '$.batters.batter[2].type'        | "^Choco.*"    | "all"            | false
        '$.batters.batter[2].type'        | "^Choco.*"    | "any"            | false
        '$.[?(@.ppu > 0.5)].name'         | "Cake"        | null             | true
        '$.[?(@.ppu > 0.5)].name'         | "Cake"        | "all"            | true
        '$.[?(@.ppu > 0.5)].name'         | "Cake"        | "any"            | true
        '$.[?(@.ppu < 0.5)].name'         | "Cake"        | null             | false
        '$.[?(@.ppu < 0.5)].name'         | "Cake"        | "all"            | false
        '$.[?(@.ppu < 0.5)].name'         | "Cake"        | "any"            | false
        '$.topping[?(@.id == 5007)].type' | "^Powdered.*" | null             | true
        '$.topping[?(@.id == 5007)].type' | "^Powdered.*" | "all"            | true
        '$.topping[?(@.id == 5007)].type' | "^Powdered.*" | "any"            | true
        '$.topping[?(@.id == 5007)].type' | "^Maple.*"    | null             | false
        '$.topping[?(@.id == 5007)].type' | "^Maple.*"    | "all"            | false
        '$.topping[?(@.id == 5007)].type' | "^Maple.*"    | "any"            | false
        '$.topping[?(@.id == 5001)].type' | "None"        | null             | true
        '$.topping[?(@.id == 5001)].type' | "None"        | "all"            | true
        '$.topping[?(@.id == 5001)].type' | "None"        | "any"            | true
        '$.topping[4:6].type'             | "^Choco.*"    | null             | true
        '$.topping[4:6].type'             | "^Choco.*"    | "all"            | true
        '$.topping[4:6].type'             | "^Choco.*"    | "any"            | true
        '$.topping[4:7].type'             | "^Choco.*"    | null             | false
        '$.topping[4:7].type'             | "^Choco.*"    | "all"            | false
        '$.topping[4:7].type'             | "^Choco.*"    | "any"            | true
        '$.size[*]'                       | "medium"      | null             | false
        '$.size[*]'                       | "medium"      | "all"            | false
        '$.size[*]'                       | "medium"      | "any"            | true
    }
}
