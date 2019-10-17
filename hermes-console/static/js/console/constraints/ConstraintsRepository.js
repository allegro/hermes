var repository = angular.module('hermes.constraints.repository', []);

repository.factory('ConstraintsRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var workloadConstraints = $resource(discovery.resolve('/workload-constraints'), {}, { query: { method: 'GET' } });
        var updateTopicConstraintsEndpoint = $resource(discovery.resolve('/workload-constraints/topic'), null, { update: { method: 'PUT' } });
        var updateSubscriptionConstraintsEndpoint = $resource(discovery.resolve('/workload-constraints/subscription'), null, { update: { method: 'PUT' } });
        var removeTopicConstraintsEndpoint = $resource(discovery.resolve('/workload-constraints/topic/:topicName'));
        var removeSubscriptionConstraintsEndpoint = $resource(discovery.resolve('/workload-constraints/subscription/:topicName/:subscriptionName'));

        return {
            getWorkloadConstraints: function () {
                return workloadConstraints.query({}).$promise
                    .then(function (response) {
                        var workloadConstraints = {};
                        workloadConstraints.topicConstraints = [];
                        for (var topicName in response.topicConstraints) {
                            workloadConstraints.topicConstraints.push({
                                topicName: topicName,
                                consumersNumber: response.topicConstraints[topicName].consumersNumber
                            });
                        }

                        workloadConstraints.subscriptionConstraints = [];
                        for (var subscriptionName in response.subscriptionConstraints) {
                            workloadConstraints.subscriptionConstraints.push({
                                subscriptionName: subscriptionName,
                                consumersNumber: response.subscriptionConstraints[subscriptionName].consumersNumber
                            });
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
            updateTopicConstraints: function (topicConstraints) {
                return updateTopicConstraintsEndpoint.update({}, topicConstraints).$promise;
            },
            updateSubscriptionConstraints: function (subscriptionConstraints) {
                return updateSubscriptionConstraintsEndpoint.update({}, subscriptionConstraints).$promise;
            },
            removeTopicConstraints: function (topicName) {
                return removeTopicConstraintsEndpoint.remove({ topicName: topicName });
            },
            removeSubscriptionConstraints: function (topicName, subscriptionName) {
                return removeSubscriptionConstraintsEndpoint.remove({ topicName: topicName, subscriptionName: subscriptionName })
            }
        };
    }]);
