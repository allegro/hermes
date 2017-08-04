package pl.allegro.tech.hermes.schema.schemarepo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import pl.allegro.tech.hermes.api.RawSchema
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.schema.BadSchemaRequestException
import pl.allegro.tech.hermes.schema.InternalSchemaRepositoryException
import pl.allegro.tech.hermes.schema.RawSchemaClient
import pl.allegro.tech.hermes.schema.SchemaVersion
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

    @Shared @Subject RawSchemaClient client

    def setupSpec() {
        def port = Ports.nextAvailable()
        wireMock = new WireMockServer(new WireMockConfiguration().port(port).usingFilesUnderClasspath("schema-repo-stub"))
        wireMock.start()
        client = new SchemaRepoRawSchemaClient(ClientBuilder.newClient(), URI.create("http://localhost:$port/schema-repo"))
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
        wireMock.stubFor(get(subjectUrl(topicName)).willReturn(notFoundResponse()))
        wireMock.stubFor(put(subjectUrl(topicName)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, getRequestedFor(subjectUrl(topicName)))
        wireMock.verify(1, putRequestedFor(subjectUrl(topicName)))
        wireMock.verify(1, putRequestedFor(registerSchemaUrl(topicName))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo(rawSchema.value())))
    }

    def "should register schema for existing subject"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, getRequestedFor(subjectUrl(topicName)))
        wireMock.verify(0, putRequestedFor(subjectUrl(topicName)))
        wireMock.verify(1, putRequestedFor(registerSchemaUrl(topicName))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo(rawSchema.value())))
    }

    def "should throw exception for unsuccessful subject registration"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName)).willReturn(notFoundResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(BadSchemaRequestException)
    }

    def "should throw exception on invalid schema registration"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName)).willReturn(badRequestResponse("some error")))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        def e = thrown(BadSchemaRequestException)
        e.message.contains("some error")
    }

    def "should throw exception on internal server error response"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should return empty optional when latest schema does not exist"() {
        when:
        def schema = client.getLatestSchema(topicName)

        then:
        !schema.isPresent()
    }

    def "should throw exception when not able to fetch latest schema"() {
        given:
        wireMock.stubFor(get(latestSchemaUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.getLatestSchema(topicName)

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should return latest schema"() {
        given:
        wireMock.stubFor(get(latestSchemaUrl(topicName)).willReturn(okResponse().withBody("0\t{}")))

        when:
        def schema = client.getLatestSchema(topicName)

        then:
        schema.get().value() == "{}"
    }

    def "should return all schema versions"() {
        given:
        wireMock.stubFor(get(allSchemasUrl(topicName)).willReturn(okResponse().withBodyFile("all-schemas-response.json")
                .withHeader("Content-Type", "application/json")))

        when:
        def versions = client.getVersions(topicName)

        then:
        versions.containsAll(SchemaVersion.valueOf(0), SchemaVersion.valueOf(1), SchemaVersion.valueOf(2))
    }

    def "should return empty list when there are no schema versions registered"() {
        when:
        def versions = client.getVersions(topicName)

        then:
        versions.isEmpty()
    }

    def "should throw exception when not able to fetch schema versions"() {
        given:
        wireMock.stubFor(get(allSchemasUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.getVersions(topicName)

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should return schema at specified version"() {
        given:
        def version = 5
        wireMock.stubFor(get(schemaVersionUrl(topicName, version)).willReturn(okResponse().withBody("0\t{}")))

        when:
        def schema = client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        schema.get().value() == "{}"
    }

    private UrlMatchingStrategy subjectUrl(TopicName topic) {
        urlEqualTo("/schema-repo/${topic.qualifiedName()}")
    }

    private UrlMatchingStrategy latestSchemaUrl(TopicName topic) {
        urlEqualTo("/schema-repo/${topic.qualifiedName()}/latest")
    }

    private UrlMatchingStrategy schemaVersionUrl(TopicName topic, int version) {
        urlEqualTo("/schema-repo/${topic.qualifiedName()}/id/$version")
    }

    private UrlMatchingStrategy allSchemasUrl(TopicName topic) {
        urlEqualTo("/schema-repo/${topic.qualifiedName()}/all")
    }

    private UrlMatchingStrategy registerSchemaUrl(TopicName topic) {
        urlEqualTo("/schema-repo/${topic.qualifiedName()}/register")
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
