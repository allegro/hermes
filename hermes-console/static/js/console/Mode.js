var modeModule = angular.module('hermes.mode', []);

modeModule.factory('Mode', ['DiscoveryService', '$http', '$rootScope',
    function (discovery, $http, $rootScope) {
        return {
            reload: function () {
                // intentionally use $http over $resource because of https://stackoverflow.com/questions/24876593/resource-query-return-split-strings-array-of-char-instead-of-a-string
                $http.get(discovery.resolve('/mode')).then(
                    function (response) {
                        $rootScope.readOnly = (response.data === 'readOnly' || response.data === 'readOnlyAdmin');
                    });
            }
        };

    }]);
