var constraints = angular.module('hermes.constraints.list', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsListController', ['ConstraintsRepository', '$scope',
    function (constraintsRepository, $scope) {
        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                $scope.topicConstraints = workloadConstraints.topicConstraints;
                $scope.subscriptionConstraints = workloadConstraints.subscriptionConstraints;
            });
    }]);
