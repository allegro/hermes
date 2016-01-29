var metrics = angular.module('hermes.subscription.metrics', ['hermes.metrics']);

metrics.factory('SubscriptionMetrics', ['DiscoveryService', '$resource', 'MetricStoreUrlResolver',
    function(discovery, $resource, metricStoreUrlResolver) {
        var metrics = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/metrics'));

        return {
            metrics: function (topicName, subscriptionName) {
                return metrics.get({topicName: topicName, subscriptionName: subscriptionName})
                    .$promise.then(function(metrics) {
                        return {
                            rate: parseFloat(metrics.rate),
                            delivered: metrics.delivered,
                            discarded: metrics.discarded,
                            lag: metrics.lag,
                            responses: {
                                '2xx': parseFloat(metrics.codes2xx),
                                '4xx': parseFloat(metrics.codes4xx),
                                '5xx': parseFloat(metrics.codes5xx),
                                errors: parseFloat(metrics.otherErrors),
                                timeouts: parseFloat(metrics.timeouts)
                            },
                        };
                    });
            },
            metricsUrls: function(groupName, topicName, subscriptionName) {
                var topicWithoutGroup = topicName.substring(groupName.length + 1, topicName.length);
                return metricStoreUrlResolver.subscriptionMetrics(groupName, topicWithoutGroup, subscriptionName);
            }
        };
    }]);
