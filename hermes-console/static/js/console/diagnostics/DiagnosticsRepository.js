var repository = angular.module('hermes.diagnostics.repository', []);

repository.factory('DiagnosticsRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {

        var consumerGroups = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/consumer-groups'),
            {}, {query:  {method: 'GET', isArray: true}});

        return {
            getConsumerGroups: function (topicName, subscriptionName) {
                return consumerGroups.query({topicName: topicName, subscriptionName: subscriptionName}).$promise;
            }
        };
    }]);
