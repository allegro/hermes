var services = angular.module('hermes.services', []);

services.service('PasswordService', ['$rootScope', function ($rootScope) {

        return {
            getRoot: function () {
                return $rootScope.rootPassword;
            },
            setRoot: function (password) {
                $rootScope.rootPassword = password;
            },
            reset: function() {
                $rootScope.rootPassword = '';
            }
        };

    }]);