var services = angular.module('hermes.services', []);

services.service('PasswordService', ['$rootScope', function ($rootScope) {

        return {
            get: function () {
                return $rootScope.password;
            },
            set: function (password) {
                $rootScope.password = password;
            },
            getRoot: function () {
                return $rootScope.rootPassword;
            },
            setRoot: function (password) {
                $rootScope.rootPassword = password;
            },
            reset: function() {
                $rootScope.password = '';
                $rootScope.rootPassword = '';
            }
        };

    }]);