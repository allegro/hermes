package pl.allegro.tech.hermes.schema

import pl.allegro.tech.hermes.api.RawSchema
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient
import spock.lang.Specification
import spock.lang.Subject

@Subject(SubjectNamingStrategy)
class SubjectNamingStrategyTest extends Specification {

    def "should create proper subject names"() {
        given:
        def topicName = new TopicName("group", "name")

        expect:
        subjectNamingStrategy.apply(topicName) == subject

        where:
        subjectNamingStrategy << [
                SubjectNamingStrategy.qualifiedName,
                SubjectNamingStrategy.qualifiedName.withValueSuffixIf(true),
                SubjectNamingStrategy.qualifiedName.withNamespacePrefixIf(true, "ns"),
                SubjectNamingStrategy.qualifiedName
                        .withValueSuffixIf(true)
                        .withNamespacePrefixIf(true, "ns"),
                SubjectNamingStrategy.qualifiedName
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
