package pl.allegro.tech.hermes.schema.schemarepo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.UrlPattern
import pl.allegro.tech.hermes.api.RawSchema
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.schema.BadSchemaRequestException
import pl.allegro.tech.hermes.schema.InternalSchemaRepositoryException
import pl.allegro.tech.hermes.schema.RawSchemaClient
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRepositoryInstanceResolver
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.put
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class SchemaRepoRawSchemaClientTest extends Specification {

    @Shared TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared RawSchema rawSchema = RawSchema.valueOf("{}")

    @Shared WireMockServer wireMock

    @Shared SchemaRepositoryInstanceResolver resolver

    @Shared SubjectNamingStrategy[] subjectNamingStrategies

    @Shared @Subject RawSchemaClient[] clients

    def setupSpec() {
        def port = Ports.nextAvailable()
        wireMock = new WireMockServer(new WireMockConfiguration().port(port).usingFilesUnderClasspath("schema-repo-stub"))
        wireMock.start()
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port/schema-repo"))
        subjectNamingStrategies = [
                SubjectNamingStrategy.qualifiedName,
                SubjectNamingStrategy.qualifiedName.withValueSuffixIf(true),
                SubjectNamingStrategy.qualifiedName.withNamespacePrefixIf(true, "test"),
                SubjectNamingStrategy.qualifiedName.withValueSuffixIf(true).withNamespacePrefixIf(true, "test")
        ]
        clients = subjectNamingStrategies.collect { new SchemaRepoRawSchemaClient(resolver, it) }
    }

    def cleanupSpec() {
        wireMock.stop()
    }

    def setup() {
        wireMock.resetMappings()
        wireMock.resetRequests()
    }

    def "should register subject and schema"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, subjectNamingStrategy)).willReturn(notFoundResponse()))
        wireMock.stubFor(put(subjectUrl(topicName,subjectNamingStrategy)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, getRequestedFor(subjectUrl(topicName, subjectNamingStrategy)))
        wireMock.verify(1, putRequestedFor(subjectUrl(topicName, subjectNamingStrategy)))
        wireMock.verify(1, putRequestedFor(registerSchemaUrl(topicName, subjectNamingStrategy))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo(rawSchema.value())))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should register schema for existing subject"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, getRequestedFor(subjectUrl(topicName, subjectNamingStrategy)))
        wireMock.verify(0, putRequestedFor(subjectUrl(topicName, subjectNamingStrategy)))
        wireMock.verify(1, putRequestedFor(registerSchemaUrl(topicName, subjectNamingStrategy))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo(rawSchema.value())))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception for unsuccessful subject registration"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, subjectNamingStrategy)).willReturn(notFoundResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(BadSchemaRequestException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception on invalid schema registration"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, subjectNamingStrategy)).willReturn(badRequestResponse("some error")))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        def e = thrown(BadSchemaRequestException)
        e.message.contains("some error")

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception on internal server error response"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return empty optional when latest schema does not exist"() {
        when:
        def schema = client.getLatestSchema(topicName)

        then:
        !schema.isPresent()

        where:
        client << clients
    }

    def "should throw exception when not able to fetch latest schema"() {
        given:
        wireMock.stubFor(get(latestSchemaUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.getLatestSchema(topicName)

        then:
        thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return latest schema"() {
        given:
        wireMock.stubFor(get(latestSchemaUrl(topicName, subjectNamingStrategy)).willReturn(okResponse().withBody("0\t{}")))

        when:
        def schema = client.getLatestSchema(topicName)

        then:
        schema.get().value() == "{}"

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return all schema versions"() {
        given:
        wireMock.stubFor(get(allSchemasUrl(topicName, subjectNamingStrategy)).willReturn(okResponse().withBodyFile("all-schemas-response.json")
                .withHeader("Content-Type", "application/json")))

        when:
        def versions = client.getVersions(topicName)

        then:
        versions.containsAll(SchemaVersion.valueOf(0), SchemaVersion.valueOf(1), SchemaVersion.valueOf(2))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return empty list when there are no schema versions registered"() {
        when:
        def versions = client.getVersions(topicName)

        then:
        versions.isEmpty()

        where:
        client << clients
    }

    def "should throw exception when not able to fetch schema versions"() {
        given:
        wireMock.stubFor(get(allSchemasUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.getVersions(topicName)

        then:
        thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return schema at specified version"() {
        given:
        def version = 5
        wireMock.stubFor(get(schemaVersionUrl(topicName, version, subjectNamingStrategy)).willReturn(okResponse().withBody("0\t{}")))

        when:
        def schema = client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        schema.get().value() == "{}"

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    private UrlPattern subjectUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/schema-repo/${subjectNamingStrategy.apply(topic)}")
    }

    private UrlPattern latestSchemaUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/schema-repo/${subjectNamingStrategy.apply(topic)}/latest")
    }

    private UrlPattern schemaVersionUrl(TopicName topic, int version, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/schema-repo/${subjectNamingStrategy.apply(topic)}/id/$version")
    }

    private UrlPattern allSchemasUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/schema-repo/${subjectNamingStrategy.apply(topic)}/all")
    }

    private UrlPattern registerSchemaUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/schema-repo/${subjectNamingStrategy.apply(topic)}/register")
    }

    private ResponseDefinitionBuilder okResponse() {
        aResponse().withStatus(200)
    }

    private ResponseDefinitionBuilder badRequestResponse(String body) {
        aResponse().withStatus(400).withBody(body)
    }

    private ResponseDefinitionBuilder notFoundResponse() {
        aResponse().withStatus(404)
    }

    private ResponseDefinitionBuilder internalErrorResponse() {
        aResponse().withStatus(500)
    }
}
