package pl.allegro.tech.hermes.management.domain.query.json
import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.management.domain.query.ParseException
import pl.allegro.tech.hermes.management.domain.query.Query
import pl.allegro.tech.hermes.management.domain.query.QueryParser
import pl.allegro.tech.hermes.management.domain.query.matcher.MatcherException
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Collectors

class JsonQueryParserTest extends Specification {

    @Subject
    QueryParser queryParser

    List<SimpleObject> colors

    List<ComplexObject> cars

    List<StatefulObject> states

    void setup() {
        colors = [
                new SimpleObject(field: "red"),
                new SimpleObject(field: "blue"),
                new SimpleObject(field: "green"),
                new SimpleObject(field: "red"),
                new SimpleObject(field: "red")
        ] as List<SimpleObject>

        cars = [
                new ComplexObject(nested: new SimpleObject(field: "Audi")),
                new ComplexObject(nested: new SimpleObject(field: "BMW")),
                new ComplexObject(nested: new SimpleObject(field: "Mercedes")),
                new ComplexObject(nested: new SimpleObject(field: "Volkswagen")),
        ] as List<ComplexObject>

        states = [
                new StatefulObject(state: State.ACTIVE),
                new StatefulObject(state: State.ACTIVE),
                new StatefulObject(state: State.ACTIVE),
                new StatefulObject(state: State.DELETED),
                new StatefulObject(state: State.DELETED)
        ] as List<StatefulObject>

        queryParser = new JsonQueryParser(new ObjectMapper())
    }

    def "should fail to parse query"() {

        given:
        def query = "{}"

        when:
        parse(query, SimpleObject)

        then:
        thrown(ParseException)
    }

    def "should fail to execute query"() {

        given:
        def query = "{\"query\": {\"unknown\": \"any\"}}"

        when:
        parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        thrown(MatcherException)
    }

    def "should parse query and match everything"() {

        given:
        def query = "{\"query\": {}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result == colors
        result.size() == colors.size()
    }

    def "should parse query and not much any object"() {

        given:
        def query = "{\"query\": {\"field\": \"black\"}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.isEmpty()
    }

    def "should parse query and match single result"() {

        given:
        def query = "{\"query\": {\"field\": \"green\"}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == 1
        result.first().field == 'green'
    }

    def "should parse query and match nested object"() {

        given:
        def query = "{\"query\": {\"nested.field\": \"BMW\"}}"

        when:
        def result = parse(query, ComplexObject)
                .filter(cars)
                .collect(Collectors.<ComplexObject>toList())

        then:
        result.size() == 1
        result.first().nested.field == 'BMW'
    }

    def "should parse query and match enum"() {

        given:
        def query = "{\"query\": {\"state\": \"DELETED\"}}"

        when:
        def result = parse(query, StatefulObject)
                .filter(states)
                .collect(Collectors.<StatefulObject>toList())

        then:
        result.size() == 2
        result*.state == [State.DELETED] * 2
    }

    private <T> Query<T> parse(String query, Class<T> object) {
        queryParser.parse(query, object)
    }

    class SimpleObject {

        String field
    }

    class ComplexObject {

        SimpleObject nested
    }

    class StatefulObject {

        State state
    }

    enum State {
        ACTIVE,
        DELETED
    }
}
