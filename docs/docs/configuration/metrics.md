# Metrics

Hermes Frontend and Consumers use [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) library to gather
and publish metrics to Metric Store.

If you would like to preview or debug metrics, set `metrics.console.reporter` to `true`, so they will be printed
to stdout.

## Graphite

By default Hermes includes Graphite reporter, which can be configured using following options:

Option                    | Description                            | Default value
------------------------- | -------------------------------------- | -------------
metrics.graphite.reporter | enable Graphite reporter               | false
graphite.host             | Graphite host                          | localhost
graphite.port             | Graphite port                          | 2003
graphite.prefix           | prefix for all metrics                 | stats.tech.hermes
report.period             | how often to report metrics in seconds | 20

In order to be able to access basic metrics via Management API, it needs to be configured to reach Graphite API:

Option                  | Description              | Default value
----------------------- | ------------------------ | -------------
metrics.graphiteHttpUri | URI to Graphite HTTP API | http://localhost:80
metrics.prefix          | prefix for all metrics   | stats.tech.hermes

## Custom

You can register any custom reporter that is compatible with Dropwizard `MetricRegistry`. Use programmatic API to do so.
The code is the same for both Frontend and Consumers module.

```java
HermesFrontend.Builder builder = HermesFrontend.frontend();
/*
    or for Consumers:
    HermesConsumerBuilder builder = HermesConsumers.consumers()
*/

builder.withStartupHook(serviceLocator -> {
    MyMetricsReporter reporter = new MyMetricsReporter(serviceLocator.getService(MetricRegistry.class));
    reporter.start();
});

```
