var services = angular.module('hermes.services');

services.service('SupportTeamService',['$resource', 'DiscoveryService',
    function($resource, discovery) {

        var service = $resource(discovery.resolve('/supportTeams/:name'));

        this.getGroups = function(groupName) {
            return service.query({name: groupName}).$promise;
        }
    }]);