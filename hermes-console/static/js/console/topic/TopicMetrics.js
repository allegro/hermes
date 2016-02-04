var metrics = angular.module('hermes.topic.metrics', ['hermes.metrics']);

metrics.factory('TopicMetrics', ['DiscoveryService', '$resource', 'MetricStoreUrlResolver',
    function(discovery, $resource, metricStoreUrlResolver) {
        var metrics = $resource(discovery.resolve('/topics/:topicName/metrics'));

        return {
            metrics: function (topicName) {
                return metrics.get({topicName: topicName})
                    .$promise.then(function(metrics) {
                        return {
                            published: metrics.published,
                            rate: parseFloat(metrics.rate),
                            deliveryRate: parseFloat(metrics.deliveryRate)
                        };
                    });
            },
            metricsUrls: function(groupName, topicName) {
                var topicWithoutGroup = topicName.substring(groupName.length + 1, topicName.length);
                return metricStoreUrlResolver.topicMetrics(groupName, topicWithoutGroup);
            }
        };
    }]);
