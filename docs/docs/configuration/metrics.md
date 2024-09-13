# Metrics

Hermes Frontend, Consumers and Management use [Micrometer Metrics](https://github.com/micrometer-metrics/micrometer) library to gather
and expose metrics.

## Prometheus
By default, Hermes includes Prometheus reporter. It exposes metrics on `/status/prometheus` endpoint. 
Reporter configuration can be configured using following options:

Option                                         | Description                                           | Default value
---------------------------------------------- |-------------------------------------------------------| -------------
{modulePrefix}.metrics.prometheus.step         | The step size to use in computing windowed statistics | 60s
{modulePrefix}.metrics.prometheus.descriptions | If meter descriptions should be sent to Prometheus    | true

In order to be able to access basic metrics via Management API, it needs to be configured to reach Prometheus API:

Option                                    | Description                                   | Default value
------------------------------------------|-----------------------------------------------| -------------
prometheus.client.enabled                 | Should fetch external metrics from Prometheus | true
prometheus.client.externalMonitoringUrl   | URI to Prometheus HTTP API                    | http://localhost:18090
