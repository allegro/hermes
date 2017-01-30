angular.module('hermes.owner', ['hermes.services'])
    .service('OwnerService', ['$resource', 'DiscoveryService',
        function ($resource, discovery) {

            var searchOwnersResource = $resource(discovery.resolve('/owners/sources/:source'));
            var getOwnerResource = $resource(discovery.resolve('/owners/sources/:source/:id'));
            var sourceNamesResource = $resource(discovery.resolve('/owners/sources'));

            this.getOwners = function (source, searchString) {
                return searchOwnersResource.query({source: source, search: searchString}).$promise;
            };

            this.getOwner = function (source, id) {
                return getOwnerResource.get({source: source, id: id}).$promise;
            };

            this.getSourceNames = function () {
                return sourceNamesResource.query().$promise;
            };

        }]);