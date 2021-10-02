angular.module('hermes.filters.repository', [])
    .factory('FiltersRepository', ['DiscoveryService', '$resource',
        function (discovery, $resource) {
            var filtersResource = $resource(discovery.resolve('/filters/:topicName'), null, {verify: {method: 'POST'}});

            return {
                verify: function (topicName, filters, message) {
                    return filtersResource.verify({topicName: topicName}, {filters: filters, message: btoa(utf8.encode(message || ""))});
                }
            };
        }]);
