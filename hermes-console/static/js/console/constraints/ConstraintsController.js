var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsController', ['ConstraintsRepository', '$scope', '$stateParams',
    function (constraintsRepository, $scope, $stateParams) {

        var _isSubscription = function (constraintsName) {
            return constraintsName.includes('$');
        };

        $scope.constraintsName = $stateParams.constraintsName;

        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                if (_isSubscription($scope.constraintsName)) {
                    $scope.constraintsType = 'Subscription';
                    $scope.consumersNumber = workloadConstraints.subscriptionConstraints
                        .find(function (cons) {
                            return cons.subscriptionName === $scope.constraintsName;
                        })
                        .consumersNumber;
                } else {
                    $scope.constraintsType = 'Topic';
                    $scope.consumersNumber = workloadConstraints.topicConstraints
                        .find(function (cons) {
                            return cons.topicName === $scope.constraintsName;
                        })
                        .consumersNumber;
                }
            });
    }]);
