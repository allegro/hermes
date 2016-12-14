var services = angular.module('hermes.services');

services.service('MaintainerService',['$resource', 'DiscoveryService',
    function($resource, discovery) {

        var service = $resource(discovery.resolve('/maintainers/:source/:searchString'));

        this.getMaintainers = function(source, searchString) {
            return service.query({source: source, searchString: searchString}).$promise;
        }
    }]);