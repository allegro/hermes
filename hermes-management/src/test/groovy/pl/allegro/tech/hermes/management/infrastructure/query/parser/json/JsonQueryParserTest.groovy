package pl.allegro.tech.hermes.management.infrastructure.query.parser.json

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.query.Query
import pl.allegro.tech.hermes.management.infrastructure.query.parser.ParseException
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParser
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Collectors

import static pl.allegro.tech.hermes.api.Topic.Builder.topic

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

    def "should return empty result for query with unknown field"() {

        given:
        def query = "{\"query\": {\"unknown\": \"any\"}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == 0
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

    def "should parse query and not match any object"() {

        given:
        def query = "{\"query\": {\"field\": \"black\"}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.empty
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

    def "should parse query and match non java bean object"() {

        given:
        def queryWithNonJavaBeanField = """{"query": {"migratedFromJsonType": true}}"""

        def topics = [
                topic().migratedFromJsonType().build(),
                topic().build(),
        ] as List<Topic>

        when:
        def result = parse(queryWithNonJavaBeanField, Topic)
                .filter(topics)
                .collect(Collectors.<Topic>toList())

        then:
        result.size() == 1
        result.get(0).wasMigratedFromJsonType() == true
    }

    def "should parse query with eq operator and match single result"() {

        given:
        def query = "{\"query\": {\"field\": {\"eq\": \"green\"}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == 1
        result.first().field == 'green'
    }

    def "should parse query with ne operator and match multiple results"() {

        given:
        def query = "{\"query\": {\"field\": {\"ne\": \"green\"}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == colors.size() - 1
        result.count {it.field == 'green'} == 0
    }

    def "should parse query with not operator and match multiple results"() {

        given:
        def query = "{\"query\": {\"not\": {\"field\": \"green\"}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == colors.size() - 1
        result.count {it.field == 'green'} == 0
    }

    def "should parse query with not eq operator and match multiple results"() {

        given:
        def query = "{\"query\": {\"not\": {\"field\": {\"eq\": \"green\"}}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == colors.size() - 1
        result.count {it.field == 'green'} == 0
    }

    def "should parse query with in operator and match single result"() {

        given:
        def query = "{\"query\": {\"field\": {\"in\": [\"green\"]}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == 1
        result.first().field == 'green'
    }

    def "should parse query with in operator and match multiple results"() {

        given:
        def query = "{\"query\": {\"field\": {\"in\": [\"red\"]}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.size() == 3
        result*.field == ['red'] * 3
    }

    def "should parse query with in operator and not match results"() {

        given:
        def query = "{\"query\": {\"field\": {\"in\": [\"black\"]}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result.empty
    }

    def "should parse query with in operator and match all results"() {

        given:
        def query = "{\"query\": {\"field\": {\"in\": [\"red\", \"blue\", \"green\"]}}}"

        when:
        def result = parse(query, SimpleObject)
                .filter(colors)
                .collect(Collectors.<SimpleObject>toList())

        then:
        result == colors
        result.size() == colors.size()
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

    def "should parse and query and match results"() {

        given:
        def query = "{\"query\": {\"and\": [{\"state\": \"DELETED\"}]}}"

        when:
        def result = parse(query, StatefulObject)
                .filter(states)
                .collect(Collectors.<StatefulObject>toList())

        then:
        result.size() == 2
        result*.state == [State.DELETED] * 2
    }

    def "should parse and query and match no results"() {

        given:
        def query = "{\"query\": {\"and\": [{\"state\": \"ACTIVE\"}, {\"state\": \"DELETED\"}]}}"

        when:
        def result = parse(query, StatefulObject)
                .filter(states)
                .collect(Collectors.<StatefulObject>toList())

        then:
        result.empty
    }

    def "should fail to parse and query"() {

        given:
        def query = "{\"query\": {\"and\": {{\"state\": \"DELETED\"}}}}"

        when:
        parse(query, StatefulObject)

        then:
        thrown(ParseException.class)
    }

    def "should parse or query and match results"() {

        given:
        def query = "{\"query\": {\"or\": [{\"state\": \"DELETED\"}]}}"

        when:
        def result = parse(query, StatefulObject)
                .filter(states)
                .collect(Collectors.<StatefulObject>toList())

        then:
        result.size() == 2
        result*.state == [State.DELETED] * 2
    }

    def "should parse or query and match multiple"() {

        given:
        def query = "{\"query\": {\"or\": [{\"state\": \"ACTIVE\"}, {\"state\": \"DELETED\"}]}}"

        when:
        def result = parse(query, StatefulObject)
                .filter(states)
                .collect(Collectors.<StatefulObject>toList())

        then:
        result.size() == states.size()
        result == states
    }

    def "should fail to parse or query"() {

        given:
        def query = "{\"query\": {\"or\": {{\"state\": \"ACTIVE\"}, {\"state\": \"DELETED\"}}}}"

        when:
        parse(query, StatefulObject)

        then:
        thrown(ParseException)
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
