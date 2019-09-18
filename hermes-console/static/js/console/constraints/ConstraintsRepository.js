var repository = angular.module('hermes.constraints.repository', []);

repository.factory('ConstraintsRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var workloadConstraints = $resource(discovery.resolve('/workload-constraints'), {}, { query: { method: 'GET' } });

        return {
            getWorkloadConstraints: function () {
                return workloadConstraints.query({}).$promise;
            }
        };
    }]);