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

            function isAdmin(value) {
                return value.includes('admin');
            }

            function isOwner(value) {
                if(isAdmin(value)) {
                    return true;
                }
                if(subscriptionName) {
                    return value.includes('subscriptionOwner');
                }
                if(topicName) {
                    return value.includes('topicOwner');
                }
                return value.includes('any');
            }

            rolesResource.query().$promise.then(
                function (value) {
                    $rootScope.admin = $rootScope.authEnabled.headers || isAdmin(value);
                    $rootScope.disabled = !$rootScope.authEnabled.headers && (!isOwner(value) || $rootScope.readOnly);
                },
                function() {
                    $rootScope.admin = false;
                    $rootScope.disabled = true;
                }
            );
        }
    };

}]);
