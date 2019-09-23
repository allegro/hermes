var repository = angular.module('hermes.constraints.repository', []);

repository.factory('ConstraintsRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var workloadConstraints = $resource(discovery.resolve('/workload-constraints'), {}, { query: { method: 'GET' } });
        var removeTopicConstraintsEndpoint = $resource(discovery.resolve('/workload-constraints/topic/:topicName'));
        var removeSubscriptionConstraintsEndpoint = $resource(discovery.resolve('/workload-constraints/subscription/:topicName/:subscriptionName'));

        return {
            getWorkloadConstraints: function () {
                return workloadConstraints.query({}).$promise
                    .then(function (response) {
                        var workloadConstraints = {};
                        workloadConstraints.topicConstraints = [];
                        for (topicName in response.topicConstraints) {
                            if (response.topicConstraints.hasOwnProperty(topicName)) {
                                workloadConstraints.topicConstraints.push({
                                    topicName: topicName,
                                    consumersNumber: response.topicConstraints[topicName].consumersNumber
                                });
                            }
                        }

                        workloadConstraints.subscriptionConstraints = [];
                        for (subscriptionName in response.subscriptionConstraints) {
                            if (response.subscriptionConstraints.hasOwnProperty(subscriptionName)) {
                                workloadConstraints.subscriptionConstraints.push({
                                    subscriptionName: subscriptionName,
                                    consumersNumber: response.subscriptionConstraints[subscriptionName].consumersNumber
                                });
                            }
                        }
                        return workloadConstraints;
                    })
                    .catch(function () {
                        return {
                            topicConstraints: [],
                            subscriptionConstraints: []
                        };
                    })
            },
            removeTopicConstraints: function (topicName) {
                return removeTopicConstraintsEndpoint.remove({ topicName: topicName });
            },
            removeSubscriptionConstraints: function (topicName, subscriptionName) {
                return removeSubscriptionConstraintsEndpoint.remove({ topicName: topicName, subscriptionName: subscriptionName })
            }
        };
    }]);
