# Monitoring
## Ensure all topic partitions are assigned to a consumer group in a subscription

To use this monitoring you have to enable it in the `hermes-management/.../application.yaml` file.
The subscriptions that have unassigned partitions are available through endpoint `/monitoring/consumer-groups`.

```yaml
monitoringConsumerGroups:
  enabled: true
  numberOfThreads: 6
  scanEvery: 120s
```

The `numberOfThreads` and `scanEvery` parameters are just examples. For the best performance of this monitoring, you need to configure them yourself.

