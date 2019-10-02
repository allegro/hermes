var rolesModule = angular.module('hermes.visibility', ['hermes.auth']);

rolesModule.factory('Visibility', ['AuthService', 'DiscoveryService', '$resource', '$rootScope',
    function (auth, discovery, $resource, $rootScope) {

    var rolesResource = $resource(discovery.resolve('/roles'));

    var service =  {

        update: function () {

            function notLoggedIn() {
                return auth.isEnabled() && !auth.isAuthorized();
            }

            function isAdmin(value) {
                return value.includes('admin');
            }

            rolesResource.query().$promise.then(
                function (value) {
                    $rootScope.admin = isAdmin(value);
                    $rootScope.disabled = !isAdmin(value) && notLoggedIn() || $rootScope.readOnly;
                }
            );
        }
    };

    service.update();

    return service;

}]);
