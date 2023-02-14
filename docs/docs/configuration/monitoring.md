# Monitoring
## Ensure all topic partitions are assigned to consumer group in subscription

To use this monitoring you have to enable it in file `application.yaml` in module `hermes-management`. 
The subscriptions that have unassigned partitions are available through endpoint `/monitoring/consumergroup`.

```yaml
monitoringConsumerGroups:
  enabled: false
  numberOfThreads: 6
  secondsBetweenScans: 120
```

The `nunmberOfThreads` and `secondsBetweenScans` parameters are examples. For the best performance of this monitoring, you need to configure them yourself.

