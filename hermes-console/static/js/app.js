var hermes = angular.module('hermes', [
    'ngResource',
    'ui.router',
    'ui.bootstrap',
    'jsonFormatter',
    'hermes.discovery',
    'hermes.home',
    'hermes.directives',
    'hermes.groups',
    'hermes.messagePreview',
    'hermes.auth',
    'hermes.search',
    'hermes.stats',
    'hermes.constraints',
    'hermes.diagnostics',
    'hermes.consistency',
    'hermes.visibility',
    'hermes.mode',
    'hermes.readiness',
]);

hermes.constant('DASHBOARD_CONFIG', config.dashboard);
hermes.constant('DISCOVERY_CONFIG', config.hermes.discovery);
hermes.constant('AUTH_CONFIG', config.auth);
hermes.constant('AUTH_OAUTH_CONFIG', config.auth.oauth);
hermes.constant('METRICS_CONFIG', config.metrics);
hermes.constant('CONSOLE_CONFIG', config.console);
hermes.constant('TOPIC_CONFIG', config.topic || {});
hermes.constant('GROUP_CONFIG', config.group || {});
hermes.constant('SUBSCRIPTION_CONFIG', config.subscription || {});
hermes.constant('OWNER_CONFIG', config.owner || {});
hermes.constant('CONSISTENCY_CONFIG', config.consistency || {});

hermes.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', '$uibTooltipProvider',
    function ($stateProvider, $urlRouterProvider, $httpProvider, $tooltipProvider) {

        $urlRouterProvider.otherwise('/');

        $stateProvider
                .state('home', {
                    url: '/',
                    templateUrl: 'partials/home.html'
                })
                .state('groups', {
                    url: '/groups',
                    templateUrl: 'partials/groups.html'
                })
                .state('group', {
                    url: '/groups/:groupName',
                    templateUrl: 'partials/group.html'
                })
                .state('topic', {
                    url: '/groups/:groupName/topics/:topicName',
                    templateUrl: 'partials/topic.html'
                })
                .state('subscription', {
                    url: '/groups/:groupName/topics/:topicName/subscriptions/:subscriptionName',
                    templateUrl: 'partials/subscription.html'
                })
                .state('diagnostics', {
                    url: '/groups/:groupName/topics/:topicName/subscriptions/:subscriptionName/diagnostics',
                    templateUrl: 'partials/diagnostics.html'
                })
                .state('constraints', {
                    url: '/constraints',
                    templateUrl: 'partials/constraints.html'
                })
                .state('consistency', {
                    url: '/consistency',
                    templateUrl: 'partials/consistency.html'
                })
                .state('groupConsistency', {
                    url: '/consistency/:groupName',
                    templateUrl: 'partials/groupConsistency.html'
                })
                .state('topicConsistency', {
                    url: '/consistency/:groupName/topics/:topicName',
                    templateUrl: 'partials/topicConsistency.html'
                })
                .state('datacenterReadiness', {
                  url: '/readiness',
                  templateUrl: 'partials/readiness.html'
                })
                .state('search', {
                    url: '/search?entity&property&operator&pattern',
                    templateUrl: 'partials/search.html'
                })
                .state('stats', {
                    url: '/stats',
                    templateUrl: 'partials/stats.html'
                });

        $httpProvider.interceptors.push(['PasswordService', 'AUTH_CONFIG', 'AuthService', function (passwordService, authConfig, AuthService) {
                return {
                    request: function (config) {
                        if (AuthService.isEnabled() && AuthService.isAuthorized()) {
                            config.headers.Authorization = 'Token ' + AuthService.getAccessToken();
                        } else if (authConfig.headers.enabled) {
                            config.headers[authConfig.headers.adminHeader] = passwordService.getRoot();
                        }
                        return config;
                    }
                };
            }]);

        $tooltipProvider.options({ placement: 'left' });
    }]);

hermes.run(['$rootScope', 'CONSOLE_CONFIG', 'AUTH_CONFIG', "$sce", 'Mode', 'Visibility',
    function ($rootScope, config, authConfig, $sce, mode, visibility) {
        $rootScope.console = {
            title: config.title,
            footer: $sce.trustAsHtml(config.footer)
        };
        $rootScope.authEnabled = {
            oauth: authConfig.oauth.enabled,
            headers: authConfig.headers.enabled
        };
        $rootScope.$on('$viewContentLoaded', function (event) {
            visibility.update();
        });
        mode.reload();
}]);
