var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsController', ['ConstraintsRepository', '$scope', '$stateParams', '$location',
    function (constraintsRepository, $scope, $stateParams, $location) {

        var isSubscription = function (constraintsName) {
            return constraintsName.includes('$');
        };

        var redirectToConstraintsList = function() {
            $location.path('/constraints-list');
        };

        $scope.constraintsName = $stateParams.constraintsName;

        $scope.remove = function () {
            if (isSubscription($scope.constraintsName)) {
                var splittedName = $scope.constraintsName.split("$");
                var topicName = splittedName[0];
                var subscriptionName = splittedName[1];
                constraintsRepository.removeSubscriptionConstraints(topicName, subscriptionName)
                    .$promise
                    .then(redirectToConstraintsList);
            } else {
                constraintsRepository.removeTopicConstraints($scope.constraintsName)
                    .$promise
                    .then(redirectToConstraintsList);
            }
        };

        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                if (isSubscription($scope.constraintsName)) {
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
