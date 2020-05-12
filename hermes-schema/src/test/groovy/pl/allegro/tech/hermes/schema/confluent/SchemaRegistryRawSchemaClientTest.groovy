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
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy
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

@Subject(SchemaRegistryRawSchemaClient.class)
class SchemaRegistryRawSchemaClientTest extends Specification {

    @Shared String schemaRegistryContentType = "application/vnd.schemaregistry.v1+json"

    @Shared TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared RawSchema rawSchema = RawSchema.valueOf("{}")

    @Shared WireMockServer wireMock

    @Shared int port

    @Shared SchemaRepositoryInstanceResolver resolver

    @Shared SubjectNamingStrategy[] subjectNamingStrategies

    @Shared RawSchemaClient[] clients

    def setupSpec() {
        port = Ports.nextAvailable()
        wireMock = new WireMockServer(port)
        wireMock.start()
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
        subjectNamingStrategies = [
                SubjectNamingStrategy.qualifiedName,
                SubjectNamingStrategy.qualifiedName.withValueSuffixIf(true),
                SubjectNamingStrategy.qualifiedName.withNamespacePrefixIf(true, "test"),
                SubjectNamingStrategy.qualifiedName.withValueSuffixIf(true).withNamespacePrefixIf(true, "test")
        ]
        clients = subjectNamingStrategies.collect { new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), it) }
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
        wireMock.stubFor(post(versionsUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, postRequestedFor(versionsUrl(topicName, subjectNamingStrategy))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw schema registration exception for invalid schema registration"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName, subjectNamingStrategy)).willReturn(badRequestResponse().withBody("Invalid schema")))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        def e = thrown(BadSchemaRequestException)
        e.message.contains("Invalid schema")

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception for server internal error response when registering schema"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should fetch schema at specified version"() {
        given:
        def version = 5
        wireMock.stubFor(get(schemaVersionUrl(topicName, version, subjectNamingStrategy)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic","id":100,"version":$version,"schema":"{}"}""")))

        when:
        def schema = client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        schema.get() == rawSchema

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception when not able to fetch schema version"() {
        given:
        def version = 3
        wireMock.stubFor(get(schemaVersionUrl(topicName, version, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should fetch latest schema version"() {
        given:
        wireMock.stubFor(get(schemaLatestVersionUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic","id":200,"version":20,"schema":"{}"}""")))

        when:
        def schema = client.getLatestSchema(topicName)

        then:
        schema.get() == rawSchema

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
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception when not able to fetch latest schema version"() {
        given:
        wireMock.stubFor(get(schemaLatestVersionUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.getLatestSchema(topicName)

        then:
        def e = thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return all schema versions"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[3,6,2]")))

        when:
        def versions = client.getVersions(topicName)

        then:
        versions.containsAll(SchemaVersion.valueOf(3), SchemaVersion.valueOf(6), SchemaVersion.valueOf(2))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should return empty list on not registered subject"() {
        when:
        def versions = client.getVersions(topicName)

        then:
        versions.isEmpty()

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception when not able to fetch schema versions"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName, subjectNamingStrategy)).willReturn(internalErrorResponse()))

        when:
        client.getVersions(topicName)

        then:
        thrown(InternalSchemaRepositoryException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should delete all schema versions"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName, subjectNamingStrategy)).willReturn(okResponse()))

        when:
        client.deleteAllSchemaVersions(topicName)

        then:
        wireMock.verify(1, deleteRequestedFor(versionsUrl(topicName, subjectNamingStrategy)))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception on method not allowed response when deleting schema"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName, subjectNamingStrategy)).willReturn(methodNotAllowedResponse()))

        when:
        client.deleteAllSchemaVersions(topicName)

        then:
        thrown(BadSchemaRequestException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should verify schema compatibility"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()
        wireMock.verify(1, postRequestedFor(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception for incompatible schema"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":false}""")))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        thrown(BadSchemaRequestException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should throw exception for 422 unprocessable entity response"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .willReturn(unprocessableEntityResponse()))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        thrown(BadSchemaRequestException)

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should accept subject not found response as if schema is valid"() {
        given:
        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .willReturn(notFoundResponse()))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()

        where:
        client << clients
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should successfully validate schema against validation endpoint"() {
        boolean validationEnabled = true
        String deleteSchemaPathSuffix = ""
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
        def client = new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), validationEnabled, deleteSchemaPathSuffix, subjectNamingStrategy)

        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        wireMock.stubFor(post(schemaValidationUrl(topicName, subjectNamingStrategy))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{ "is_valid": true}""")))

        when:
        client.validateSchema(topicName, rawSchema)

        then:
        noExceptionThrown()

        where:
        subjectNamingStrategy << subjectNamingStrategies
    }

    def "should receive errors from validation endpoint"() {
        given:
        boolean validationEnabled = true
        String deleteSchemaPathSuffix = ""
        resolver = new DefaultSchemaRepositoryInstanceResolver(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
        def client = new SchemaRegistryRawSchemaClient(resolver, new ObjectMapper(), validationEnabled, deleteSchemaPathSuffix, subjectNamingStrategy)

        wireMock.stubFor(post(schemaCompatibilityUrl(topicName, subjectNamingStrategy))
                .willReturn(okResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""{"is_compatible":true}""")))

        wireMock.stubFor(post(schemaValidationUrl(topicName, subjectNamingStrategy))
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

        where:
        subjectNamingStrategy << subjectNamingStrategies
    }

    private UrlPattern versionsUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/subjects/${subjectNamingStrategy.apply(topic)}/versions")
    }

    private UrlPattern schemaVersionUrl(TopicName topic, int version, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/subjects/${subjectNamingStrategy.apply(topic)}/versions/$version")
    }

    private UrlPattern schemaLatestVersionUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/subjects/${subjectNamingStrategy.apply(topic)}/versions/latest")
    }

    private UrlPattern schemaCompatibilityUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/compatibility/subjects/${subjectNamingStrategy.apply(topic)}/versions/latest")
    }

    private UrlPattern schemaValidationUrl(TopicName topic, SubjectNamingStrategy subjectNamingStrategy) {
        urlEqualTo("/subjects/${subjectNamingStrategy.apply(topic)}/validation")
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
