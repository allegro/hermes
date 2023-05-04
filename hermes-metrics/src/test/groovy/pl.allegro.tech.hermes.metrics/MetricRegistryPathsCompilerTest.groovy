package pl.allegro.tech.hermes.metrics

import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.hermes.metrics.PathContext.pathContext

class MetricRegistryPathsCompilerTest extends Specification {

    @Shared
    def pathsCompiler = new MetricRegistryPathsCompiler("localhost.localdomain")

    def "should compile path with hostname"() {
        expect:
        pathsCompiler.compile(MetricRegistryPathsCompiler.HOSTNAME + ".counter") == "localhost_localdomain.counter"
    }

    def "should compile path with path context"() {
        given:
        def pathContext = pathContext().withGroup("group")
                .withTopic("topic")
                .withSubscription("subscription")
                .withPartition(0)
                .withHttpCode(201)
                .build()

        when:
        def compiled = pathsCompiler.compile("hermes.$MetricRegistryPathsCompiler.GROUP.$MetricRegistryPathsCompiler.TOPIC.$MetricRegistryPathsCompiler.SUBSCRIPTION.$MetricRegistryPathsCompiler.PARTITION.$MetricRegistryPathsCompiler.HTTP_CODE", pathContext)

        then:
        compiled == "hermes.group.topic.subscription.0.201"
    }
}
