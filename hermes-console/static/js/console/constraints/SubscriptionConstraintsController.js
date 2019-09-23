var constraints = angular.module('hermes.subscription.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('SubscriptionConstraintsController', ['ConstraintsRepository', '$scope', '$stateParams',
    function (constraintsRepository, $scope, $stateParams) {
        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                $scope.subscriptionConstraints = workloadConstraints.subscriptionConstraints
                    .find(function (subscriptionCons) {
                        return subscriptionCons.subscriptionName === $stateParams.subscriptionName;
                    });
            });
    }]);
