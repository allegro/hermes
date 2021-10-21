var rolesModule = angular.module('hermes.visibility', []);

rolesModule.factory('Visibility', ['DiscoveryService', '$resource', '$rootScope', '$stateParams',
    function (discovery, $resource, $rootScope, $stateParams) {

    return   {
        update: function () {

            var topicName = $stateParams.topicName;
            var subscriptionName = $stateParams.subscriptionName;

            var topicPath = topicName && '/topics/' + topicName || '';
            var subscriptionPath = topicPath && subscriptionName && '/subscriptions/' + subscriptionName || '';

            var rolesResource = $resource(discovery.resolve('/roles' + topicPath + subscriptionPath));

            function isAdmin(rolesList) {
                return rolesList.includes('admin');
            }

            function hasSufficientPrivileges(rolesList) {
                if (isAdmin(rolesList)) {
                    return true;
                }
                if (subscriptionName) {
                    return rolesList.includes('subscriptionOwner');
                }
                if (topicName) {
                    return rolesList.includes('topicOwner');
                }
                return rolesList.includes('any');
            }

            rolesResource.query().$promise.then(
                function (commaSeparatedRoles) {
                    const rolesList = commaSeparatedRoles.toString().split(',');
                    $rootScope.admin = isAdmin(rolesList);
                    $rootScope.userHasSufficientPrivileges = hasSufficientPrivileges(rolesList);
                    $rootScope.userWithoutAccess = !$rootScope.userHasSufficientPrivileges || $rootScope.readOnly
                },
                function() {
                    $rootScope.admin = false;
                    $rootScope.userHasSufficientPrivileges = false;
                    $rootScope.userWithoutAccess = true;
                }
            );
        }
    };

}]);
