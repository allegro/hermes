var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsController', ['ConstraintsRepository', '$scope',
    function (constraintsRepository, $scope) {
        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                $scope.topicConstraints = workloadConstraints.topicConstraints;
                $scope.subscriptionConstraints = workloadConstraints.subscriptionConstraints;
            });
    }]);
