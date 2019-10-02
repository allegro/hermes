var modeModule = angular.module('hermes.mode', []);

modeModule.factory('Mode', ['DiscoveryService', '$resource', '$rootScope',
    function (discovery, $resource, $rootScope) {

        var modeResource = $resource(discovery.resolve('/mode'));

        modeResource.query().$promise.then(
            function (value) {
                $rootScope.readOnly = value !== 'readOnly'
            });

    }]);
