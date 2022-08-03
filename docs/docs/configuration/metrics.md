# Metrics

Hermes Frontend and Consumers use [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) library to gather
and publish metrics to Metric Store.

If you would like to preview or debug metrics, set `{modulePrefix}.metrics.consoleReporterEnabled` to `true`, so they will be printed
to stdout.

## Graphite

By default, Hermes includes Graphite reporter, which can be configured using following options:

Option                                         | Description                                  | Default value
---------------------------------------------- | -------------------------------------- | -------------
{modulePrefix}.metrics.graphiteReporterEnabled | enable Graphite reporter               | false
{modulePrefix}.graphite.host                   | Graphite host                          | localhost
{modulePrefix}.graphite.port                   | Graphite port                          | 2003
{modulePrefix}.graphite.prefix                 | prefix for all metrics                 | stats.tech.hermes
{modulePrefix}.metrics.reportPeriod            | how often to report metrics            | 20s

In order to be able to access basic metrics via Management API, it needs to be configured to reach Graphite API:

Option                  | Description              | Default value
----------------------- | ------------------------ | -------------
metrics.graphiteHttpUri | URI to Graphite HTTP API | http://localhost:80
metrics.prefix          | prefix for all metrics   | stats.tech.hermes

## Custom

You can register any custom reporter that is compatible with Dropwizard `MetricRegistry`.

For the Consumers and Frontend modules register the reporter as a bean, for example:

```java
@Configuration
public class CustomHermesConsumersConfiguration {

    @Bean
    @Primary
    public MetricRegistry myMetricRegistry(MetricRegistry metricRegistry) {
        return new MyMetricsReporter(metricRegistry);
    }
}
```
