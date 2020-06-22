package pl.allegro.tech.hermes.schema


import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Specification
import spock.lang.Subject

import static pl.allegro.tech.hermes.schema.SubjectNamingStrategy.qualifiedName

@Subject(SubjectNamingStrategy)
class SubjectNamingStrategyTest extends Specification {

    static def namespace = new SubjectNamingStrategy.Namespace("ns", "_")

    def "should create proper subject names"() {
        given:
        def topicName = new TopicName("group", "name")

        expect:
        subjectNamingStrategy.apply(topicName) == subject

        where:
        subjectNamingStrategy << [
                qualifiedName,
                qualifiedName.withValueSuffixIf(true),
                qualifiedName.withNamespacePrefixIf(true, namespace),
                qualifiedName
                        .withValueSuffixIf(true)
                        .withNamespacePrefixIf(true, namespace),
                qualifiedName
                        .withNamespacePrefixIf(true, namespace)
                        .withValueSuffixIf(true)
        ]
        subject << [
                "group.name",
                "group.name-value",
                "ns_group.name",
                "ns_group.name-value",
                "ns_group.name-value"
        ]
    }
}
