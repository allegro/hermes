var service = angular.module('hermes.subscriptionOffsets.service', ['hermes.discovery']);

repository.factory('SubscriptionService', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var subscriptionOffsetsEndpoint = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/moveOffsetsToTheEnd'), null, {
            query: {
                method: 'POST',
                isArray: true
            }
        });

        return {
            moveOffsets: function (topicName, subscriptionName) {
                return subscriptionOffsetsEndpoint.save({topicName: topicName, subscriptionName: subscriptionName}, {}).$promise;
            }
        };
    }]);
