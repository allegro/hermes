package pl.allegro.tech.hermes.schema.confluent

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class SchemaRegistryRawSchemaClientTest extends Specification {

    @Shared String schemaRegistryContentType = "application/vnd.schemaregistry.v1+json"

    @Shared TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared RawSchema rawSchema = RawSchema.valueOf("{}")

    @Shared WireMockServer wireMock

    @Shared int port

    @Shared SchemaRepositoryInstanceResolver resolver

    @Shared @Subject RawSchemaClient client

    // client with enabled suffixed subject naming strategy
    @Shared @Subject RawSchemaClient suffixedClient

    def setupSpec() {
        port = Ports.nextAvailable()
        wireMock = new WireMockServer(port)
        wireMock.start()
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
        client = new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), false)
        suffixedClient = new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), true)
    }

    def cleanupSpec() {
        wireMock.stop()
    }

    def setup() {
        wireMock.resetMappings()
        wireMock.resetRequests()
    }

    def "should register schema"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, postRequestedFor(versionsUrl(topicName))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))
    }

    def "should register schema with suffixed subject name"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName, true)).willReturn(okResponse()))

        when:
        suffixedClient.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, postRequestedFor(versionsUrl(topicName, true))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))
    }

    def "should throw schema registration exception for invalid schema registration"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName)).willReturn(badRequestResponse().withBody("Invalid schema")))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        def e = thrown(BadSchemaRequestException)
        e.message.contains("Invalid schema")
    }

    def "should throw exception for server internal error response when registering schema"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should fetch schema at specified version"() {
        given:
        def version = 5
        wireMock.stubFor(get(schemaVersionUrl(topicName, version)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic","id":100,"version":$version,"schema":"{}"}""")))

        when:
        def schema = client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        schema.get() == rawSchema
    }

    def "should fetch schema with suffixed subject name at specified version"() {
        given:
        def version = 5
        wireMock.stubFor(get(schemaVersionUrl(topicName, version, true)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic-value","id":100,"version":$version,"schema":"{}"}""")))

        when:
        def schema = suffixedClient.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        schema.get() == rawSchema
    }

    def "should throw exception when not able to fetch schema version"() {
        given:
        def version = 3
        wireMock.stubFor(get(schemaVersionUrl(topicName, version)).willReturn(internalErrorResponse()))

        when:
        client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should fetch latest schema version"() {
        given:
        wireMock.stubFor(get(schemaLatestVersionUrl(topicName)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic","id":200,"version":20,"schema":"{}"}""")))

        when:
        def schema = client.getLatestSchema(topicName)

        then:
        schema.get() == rawSchema
    }

    def "should return empty optional when latest schema does not exist"() {
        when:
        def schema = client.getLatestSchema(topicName)

        then:
        !schema.isPresent()
    }

    def "should throw exception when not able to fetch latest schema version"() {
        given:
        wireMock.stubFor(get(schemaLatestVersionUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.getLatestSchema(topicName)

        then:
        def e = thrown(InternalSchemaRepositoryException)
    }

    def "should return all schema versions"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName)).willReturn(okResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[3,6,2]")))

        when:
        def versions = client.getVersions(topicName)

        then:
        versions.containsAll(SchemaVersion.valueOf(3), SchemaVersion.valueOf(6), SchemaVersion.valueOf(2))
    }

    def "should return all schema versions of schema with suffixed subject name"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName, true)).willReturn(okResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[3,6,2]")))

        when:
        def versions = suffixedClient.getVersions(topicName)

        then:
        versions.containsAll(SchemaVersion.valueOf(3), SchemaVersion.valueOf(6), SchemaVersion.valueOf(2))
    }

    def "should return empty list on not registered subject"() {
        when:
        def versions = client.getVersions(topicName)

        then:
        versions.isEmpty()
    }

    def "should return empty list on not registered subject with suffix"() {
        when:
        def versions = suffixedClient.getVersions(topicName)

        then:
        versions.isEmpty()
    }

    def "should throw exception when not able to fetch schema versions"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.getVersions(topicName)

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should throw exception when not able to fetch schema versions for schema with suffixed subject"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName, true)).willReturn(internalErrorResponse()))

        when:
        suffixedClient.getVersions(topicName)

        then:
        thrown(InternalSchemaRepositoryException)
    }

    def "should delete all schema versions"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName)).willReturn(okResponse()))

        when:
        client.deleteAllSchemaVersions(topicName)

        then:
        wireMock.verify(1, deleteRequestedFor(versionsUrl(topicName)))
    }

    def "should delete all schema versions for schema with suffixed name"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName, true)).willReturn(okResponse()))

        when:
        suffixedClient.deleteAllSchemaVersions(topicName)

        then:
        wireMock.verify(1, deleteRequestedFor(versionsUrl(topicName, true)))
    }

    def "should throw exception on method not allowed response when deleting schema"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName)).willReturn(methodNotAllowedResponse()))

        when:
        client.deleteAllSchemaVersions(topicName)

        then:
        thrown(BadSchemaRequestException)
    }

    def "should throw exception on method not allowed response when deleting schema for schema with suffixed name"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName, true)).willReturn(methodNotAllowedResponse()))

        when:
        suffixedClient.deleteAllSchemaVersions(topicName)

        then:
        thrown(BadSchemaRequestException)
    }

    def "should verify schema compatibility"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()
        wireMock.verify(1, postRequestedFor(schemaCompatibilityUrl(topicName))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))
    }

    def "should verify schema compatibility for schema with suffixed subject name"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, true))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        when:
        suffixedClient.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()
        wireMock.verify(1, postRequestedFor(schemaCompatibilityUrl(topicName, true))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))
    }

    def "should throw exception for incompatible schema"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":false}""")))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        thrown(BadSchemaRequestException)
    }

    def "should throw exception for 422 unprocessable entity response"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName))
                .willReturn(unprocessableEntityResponse()))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        thrown(BadSchemaRequestException)
    }

    def "should accept subject not found response as if schema is valid"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName))
                .willReturn(notFoundResponse()))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()
    }

    def "should successfully validate schema against validation endpoint"() {
        boolean validationEnabled = true
        String deleteSchemaPathSuffix = ""
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
        client = new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), validationEnabled, deleteSchemaPathSuffix, false)

        wireMock.stubFor(post(schemaCompatibilityUrl(topicName))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        wireMock.stubFor(post(schemaValidationUrl(topicName))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{ "is_valid": true}""")))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()
    }

    def "should receive errors from validation endpoint"() {
        given:
        boolean validationEnabled = true
        String deleteSchemaPathSuffix = ""
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
        client = new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), validationEnabled, deleteSchemaPathSuffix, false)

        wireMock.stubFor(post(schemaCompatibilityUrl(topicName))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        wireMock.stubFor(post(schemaValidationUrl(topicName))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(
                                """ |
                    |{ "is_valid": false,
                    |  "errors": [
                    |    {"message": "missing doc field", "ignoredField": true},
                    |    {"message": "name should start with uppercase"}
                    |  ]
                    |}""".stripMargin())))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        def e = thrown(BadSchemaRequestException)
        e.message.contains("missing doc field")
        e.message.contains("name should start with uppercase")
    }

    private UrlPattern versionsUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/subjects/${prepareName(topic, addSuffix)}/versions")
    }

    private UrlPattern schemaVersionUrl(TopicName topic, int version, boolean addSuffix = false) {
        urlEqualTo("/subjects/${prepareName(topic, addSuffix)}/versions/$version")
    }

    private UrlPattern schemaLatestVersionUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/subjects/${prepareName(topic, addSuffix)}/versions/latest")
    }

    private UrlPattern schemaCompatibilityUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/compatibility/subjects/${prepareName(topic, addSuffix)}/versions/latest")
    }

    private UrlPattern schemaValidationUrl(TopicName topic, boolean addSuffix = false) {
        urlEqualTo("/subjects/${prepareName(topic, addSuffix)}/validation")
    }

    private String prepareName(TopicName topic, boolean addSuffix) {
        if (addSuffix) return topic.qualifiedName() + "-value";
        return topic.qualifiedName()
    }

    private ResponseDefinitionBuilder okResponse() {
        aResponse().withStatus(200)
    }

    private ResponseDefinitionBuilder badRequestResponse() {
        aResponse().withStatus(400)
    }

    private ResponseDefinitionBuilder notFoundResponse() {
        aResponse().withStatus(404)
    }

    private ResponseDefinitionBuilder methodNotAllowedResponse() {
        aResponse().withStatus(405)
    }

    private ResponseDefinitionBuilder unprocessableEntityResponse() {
        aResponse().withStatus(422)
    }

    private ResponseDefinitionBuilder internalErrorResponse() {
        aResponse().withStatus(500)
    }
}
