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

    @Shared @Subject RawSchemaClient client

    // client with enabled suffixed subject naming strategy
    @Shared @Subject RawSchemaClient suffixedClient

    def setupSpec() {
        def port = Ports.nextAvailable()
        wireMock = new WireMockServer(new WireMockConfiguration().port(port).usingFilesUnderClasspath("schema-repo-stub"))
        wireMock.start()
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port/schema-repo"))
        client = new SchemaRepoRawSchemaClient(resolver, false)
        suffixedClient = new SchemaRepoRawSchemaClient(resolver, true)
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

    def "should register subject and schema for subject with suffixed name"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, true)).willReturn(notFoundResponse()))
        wireMock.stubFor(put(subjectUrl(topicName, true)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, true)).willReturn(okResponse()))

        when:
        suffixedClient.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, getRequestedFor(subjectUrl(topicName, true)))
        wireMock.verify(1, putRequestedFor(subjectUrl(topicName, true)))
        wireMock.verify(1, putRequestedFor(registerSchemaUrl(topicName, true))
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

    def "should register schema for existing subject for schema with suffixed subject"() {
        given:
        wireMock.stubFor(get(subjectUrl(topicName, true)).willReturn(okResponse()))
        wireMock.stubFor(put(registerSchemaUrl(topicName, true)).willReturn(okResponse()))

        when:
        suffixedClient.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, getRequestedFor(subjectUrl(topicName, true)))
        wireMock.verify(0, putRequestedFor(subjectUrl(topicName, true)))
        wireMock.verify(1, putRequestedFor(registerSchemaUrl(topicName, true))
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

    def "should return empty optional when latest schema with suffixed name does not exist"() {
        when:
        def schema = suffixedClient.getLatestSchema(topicName)

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

    def "should return latest schema of schema with suffixed subject name"() {
        given:
        wireMock.stubFor(get(latestSchemaUrl(topicName, true)).willReturn(okResponse().withBody("0\t{}")))

        when:
        def schema = suffixedClient.getLatestSchema(topicName)

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

    def "should return all schema versions for schemas with suffixed subjects"() {
        given:
        wireMock.stubFor(get(allSchemasUrl(topicName, true)).willReturn(okResponse().withBodyFile("all-schemas-response.json")
                .withHeader("Content-Type", "application/json")))

        when:
        def versions = suffixedClient.getVersions(topicName)

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

    private UrlPattern subjectUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/schema-repo/${prepareName(topic, addSuffix)}")
    }

    private UrlPattern latestSchemaUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/schema-repo/${prepareName(topic, addSuffix)}/latest")
    }

    private UrlPattern schemaVersionUrl(TopicName topic, int version, boolean addSuffix = false) {
        urlEqualTo("/schema-repo/${prepareName(topic, addSuffix)}/id/$version")
    }

    private UrlPattern allSchemasUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/schema-repo/${prepareName(topic, addSuffix)}/all")
    }

    private UrlPattern registerSchemaUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/schema-repo/${prepareName(topic, addSuffix)}/register")
    }

    private String prepareName(TopicName topic, boolean addSuffix) {
        if (addSuffix) return topic.qualifiedName() + "-value";
        return topic.qualifiedName()
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
