var health = angular.module('hermes.subscription.health', []);

health.factory('SubscriptionHealth', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var health = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/health'));

        return {
            health: function (topicName, subscriptionName) {
                return health.get({topicName: topicName, subscriptionName: subscriptionName})
                    .$promise.then(function (health) {
                        var problemOccurs = function (problemCode) {
                            return _.some(health.problems, {code: problemCode})
                        };

                        return {
                            status: health.status,
                            problems: {
                                lagging: problemOccurs('LAGGING'),
                                malfunctioning: problemOccurs('MALFUNCTIONING'),
                                receivingMalformedMessages: problemOccurs('RECEIVING_MALFORMED_MESSAGES'),
                                timingOut: problemOccurs('TIMING_OUT'),
                                unreachable: problemOccurs('UNREACHABLE')
                            }
                        };
                    });
            }
        };
    }
]);
