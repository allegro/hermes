var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap'
]);

constraints.controller('ConstraintsController', ['$scope',
    function ($scope) {
        $scope.hello = 'Hello constraints controller!';
    }]);