var home = angular.module('hermes.home', []);

home.controller('HomeController', ['$scope', 'DASHBOARD_CONFIG', function($scope, config) {

    $scope.statsDashboard = config.metrics;
    $scope.docs = config.docs;

}]);
