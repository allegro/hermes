var services = angular.module('hermes.services');

services.service('MaintainerService',['$resource', 'DiscoveryService',
    function($resource, discovery) {

        var searchMaintainersResource = $resource(discovery.resolve('/maintainers/sources/:source/:searchString'));
        var sourceNamesResource = $resource(discovery.resolve('/maintainers/sources'));

        this.getMaintainers = function(source, searchString) {
            return searchMaintainersResource.query({source: source, searchString: searchString}).$promise;
        };

        this.getSourceNames = function() {
            return sourceNamesResource.query().$promise;
        };

    }]);