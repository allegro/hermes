var auth = angular.module('hermes.auth', []);

auth.controller('AuthController', ['$scope', '$rootScope', 'AuthService', 'toaster', function($scope, $rootScope, AuthService, toaster) {

    $scope.enabled = AuthService.isEnabled();
    if(!AuthService.isEnabled()) {
        // abandon any actions if OAuth is not enabled
        return;
    }

    AuthService.init();

    $rootScope.isAuthorized = AuthService.isAuthorized();

    $scope.signIn = function() {
        AuthService.login().then(function() {
            $rootScope.isAuthorized = true;
            $rootScope.$apply();
        }, function(e) {
            toaster.pop('error', 'An error occurred', AuthService.parseErrorMessage(e));
            $rootScope.$apply();
        });
    };

    $scope.signOut = function() {
        AuthService.logout().then(function() {
            $rootScope.isAuthorized = false;
            $rootScope.$apply();
        }, function() {
            toaster.pop('error', 'An error occurred',  AuthService.parseErrorMessage(e));
            $rootScope.$apply();
        });
    };
}]);

auth.service('AuthService', ['AUTH_OAUTH_CONFIG', function(config) {

    this.init = function() {
        hello.init({
            auth: {
                name: 'auth',
                oauth: {
                    version: 2,
                    auth: config.url
                },
                scope: {
                    basic: ''
                },
                login: function(p) {
                    p.options.window_width = 500;
                    p.options.window_height = 400;
                }
            }
        });
        hello.init({
            auth: config.scope
        });
    };

    this.login = function(config) {
        var options = angular.extend({display: 'popup', scope: config.scope}, config);
        return hello('auth').login(options);
    };

    this.logout = function() {
        return hello('auth').logout();
    };

    this.parseErrorMessage = function(response) {
        if (response && response.error && response.error.message) {
            // messages are in form of "text+text+text+text+text"
            return response.error.message.replace(/\+/g, ' ');
        }
        if (response && response.error && response.error.code) {
            // error codes are in form of "text_text"
            return response.error.code.replace(/_/g, ' ');
        }
        return 'Unknown error';
    };

    this.isEnabled = function() {
        return config.enabled;
    }

    this.isAuthorized = function() {
        if(config.enabled) {
            var response = hello('auth').getAuthResponse();
            var now = new Date().getTime() / 1000;
            // this is just a soft validation for UI, token must be validated on server
            return response && response.access_token && response.expires > now;
        }
        return false;
    };

    this.getAccessToken = function() {
        return hello('auth').getAuthResponse().access_token;
    };
}]);

auth.directive('authPane', function() {
    return {
        restrict: 'E',
        templateUrl: 'partials/authPane.html',
        controller: 'AuthController'
    };
});
