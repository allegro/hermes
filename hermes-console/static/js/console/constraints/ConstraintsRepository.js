var repository = angular.module('hermes.constraints.repository', []);

repository.factory('ConstraintsRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var workloadConstraints = $resource(discovery.resolve('/workload-constraints'), {}, { query: { method: 'GET' } });

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
            }
        };
    }]);
