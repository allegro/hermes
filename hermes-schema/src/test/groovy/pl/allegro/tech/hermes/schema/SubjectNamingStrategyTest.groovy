package pl.allegro.tech.hermes.schema


import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Specification
import spock.lang.Subject

import static pl.allegro.tech.hermes.schema.SubjectNamingStrategy.qualifiedName

@Subject(SubjectNamingStrategy)
class SubjectNamingStrategyTest extends Specification {

    def "should create proper subject names"() {
        given:
        def topicName = new TopicName("group", "name")

        expect:
        subjectNamingStrategy.apply(topicName) == subject

        where:
        subjectNamingStrategy << [
                qualifiedName,
                qualifiedName.withValueSuffixIf(true),
                qualifiedName.withNamespacePrefixIf(true, "ns"),
                qualifiedName
                        .withValueSuffixIf(true)
                        .withNamespacePrefixIf(true, "ns"),
                qualifiedName
                        .withNamespacePrefixIf(true, "ns")
                        .withValueSuffixIf(true)
        ]
        subject << [
                "group.name",
                "group.name-value",
                "ns.group.name",
                "ns.group.name-value",
                "ns.group.name-value"
        ]
    }
}
