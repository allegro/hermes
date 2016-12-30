var services = angular.module('hermes.services');

services.service('MaintainerService',['$resource', 'DiscoveryService',
    function($resource, discovery) {

        var searchMaintainersResource = $resource(discovery.resolve('/maintainers/sources/:source'));
        var getMaintainerResource = $resource(discovery.resolve('/maintainers/sources/:source/:id'));
        var sourceNamesResource = $resource(discovery.resolve('/maintainers/sources'));

        this.getMaintainers = function(source, searchString) {
            return searchMaintainersResource.query({source: source, search: searchString}).$promise;
        };

        this.getMaintainer = function(source, id) {
            return getMaintainerResource.get({source: source, id: id}).$promise;
        };

        this.getSourceNames = function() {
            return sourceNamesResource.query().$promise;
        };

    }]);