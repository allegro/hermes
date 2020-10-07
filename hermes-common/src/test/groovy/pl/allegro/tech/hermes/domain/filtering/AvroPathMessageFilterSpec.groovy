package pl.allegro.tech.hermes.domain.filtering

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.domain.filtering.avro.AvroPathSubscriptionMessageFilterCompiler
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
                     "description": "Maple syrup",
                     "ingredients": ["corn syrup", "water"]
                }
            }
        '''

        def avro = new JsonAvroConverter().convertToAvro(json.bytes, schema)
        def spec = new MessageFilterSpecification([path: path, matcher: matcher])
        def msg = FilterableMessageBuilder
                .withTestMessage()
                .withContent(avro)
                .withSchema(schema, 1, 0)
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
        ".batter.ingredients"     | "null"      | true
        ".batter.ingredients"     | ".*"        | true
        ".batter.ingredients"     | "sugar"     | false
        ".topping.ingredients"    | ".*syrup.*" | true
    }

    @Unroll
    def "array paths"(String path, String matcher, String matchingStrategy, boolean result) {
        given:
        def schema = AvroUserSchemaLoader.load("/movie.avsc")

        def json = '''
            {
                "id": "0001",
                "title": {
                    "main": "The Godfather",
                    "aliases": ["Godfather, The", "Godfather", "El padrino" ]
                },
                "type": "drama",
                "cast": [
                    {
                        "id": "31",
                        "firstName": "Marlon",
                        "lastName": "Brando",
                        "character": "Don Vito Corleone",
                        "awards": ["Oscar for Best Actor in a Leading Role", "Golden Globe for Best Actor in a Motion Picture - Drama"]
                    },
                    {
                        "id": "22",
                        "firstName": "Al",
                        "lastName": "Pacino",
                        "character": "Michael Corleone",
                        "awards": ["NSFC Award for Best Actor", "NBR Award for Best Supporting Actor"]
                    }
                ]
            }
        '''

        def avro = new JsonAvroConverter().convertToAvro(json.bytes, schema)
        def spec = new MessageFilterSpecification([path: path, matcher: matcher, matchingStrategy: matchingStrategy])
        def msg = FilterableMessageBuilder
            .withTestMessage()
            .withContent(avro)
            .withSchema(schema, 1, 0)
            .withContentType(ContentType.AVRO)
            .build()

        expect:
        result == new AvroPathSubscriptionMessageFilterCompiler().compile(spec).test(msg)

        where:
        path                 | matcher        | matchingStrategy | result
        ".cast[*].lastName"  | "Pacino"       | null             | false
        ".cast[*].lastName"  | "Pacino"       | "all"            | false
        ".cast[*].lastName"  | "Pacino"       | "any"            | true
        ".cast[*].character" | ".*Corleone.*" | null             | true
        ".cast[*].character" | ".*Corleone.*" | "all"            | true
        ".cast[*].character" | ".*Corleone.*" | "any"            | true
        ".cast[*].firstName" | "unknown"      | null             | false
        ".cast[*].firstName" | "unknown"      | "all"            | false
        ".cast[*].firstName" | "unknown"      | "any"            | false
        ".cast[0].lastName"  | "Brando"       | null             | true
        ".cast[0].lastName"  | "Brando"       | "all"            | true
        ".cast[0].lastName"  | "Brando"       | "any"            | true
        ".cast[1].lastName"  | "Pacino"       | null             | true
        ".cast[1].lastName"  | "Pacino"       | "all"            | true
        ".cast[1].lastName"  | "Pacino"       | "any"            | true
        ".cast[2].lastName"  | "Pacino"       | null             | false
        ".cast[2].lastName"  | "Pacino"       | "all"            | false
        ".cast[2].lastName"  | "Pacino"       | "any"            | false
        ".cast[*].awards[*]" | ".*Best.*"     | null             | true
        ".cast[*].awards[*]" | ".*Best.*"     | "all"            | true
        ".cast[*].awards[*]" | ".*Best.*"     | "any"            | true
        ".cast[1].awards[*]" | ".*Best.*"     | null             | true
        ".cast[1].awards[*]" | ".*Best.*"     | "all"            | true
        ".cast[1].awards[*]" | ".*Best.*"     | "any"            | true
        ".cast[10]"          | "null"         | null             | true
        ".cast[10]"          | "null"         | "all"            | true
        ".cast[10]"          | "null"         | "any"            | true
        ".cast[10]"          | "dummy string" | null             | false
        ".cast[10]"          | "dummy string" | "all"            | false
        ".cast[10]"          | "dummy string" | "any"            | false
        ".title.aliases[5]"  | "null"         | null             | true
        ".title.aliases[5]"  | "null"         | "all"            | true
        ".title.aliases[5]"  | "null"         | "any"            | true
        ".title.aliases[5]"  | "some title"   | null             | false
        ".title.aliases[5]"  | "some title"   | "all"            | false
        ".title.aliases[5]"  | "some title"   | "any"            | false
        ".title.aliases[1]"  | "Godfather"    | null             | true
        ".title.aliases[1]"  | "Godfather"    | "all"            | true
        ".title.aliases[1]"  | "Godfather"    | "any"            | true
        ".title.aliases[*]"  | "Godfather"    | null             | false
        ".title.aliases[*]"  | "Godfather"    | "all"            | false
        ".title.aliases[*]"  | "Godfather"    | "any"            | true
        ".title.aliases[*]"  | "^[GTE].*"     | null             | true
        ".title.aliases[*]"  | "^[GTE].*"     | "all"            | true
        ".title.aliases[*]"  | "^[GTE].*"     | "any"            | true
    }

    def "should throw exception for malformed message"() {
        given:
        def schema = AvroUserSchemaLoader.load("/cake.avsc")
        def invalidContent = new byte[10]

        when:
        new AvroPathSubscriptionMessageFilterCompiler().compile(new MessageFilterSpecification([path: ".id", matcher: "0001"]))
                .test(FilterableMessageBuilder
                .withTestMessage()
                .withContent(invalidContent)
                .withSchema(schema, 1,0)
                .build())

        then:
        thrown FilteringException
    }
}


