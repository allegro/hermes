var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsController', ['ConstraintsRepository', '$scope', '$stateParams', '$location', '$uibModal',
    function (constraintsRepository, $scope, $stateParams, $location, $modal) {

        var isSubscription = function (constraintsName) {
            return constraintsName.includes('$');
        };

        var redirectToConstraintsList = function() {
            $location.path('/constraints-list');
        };

        $scope.constraintsName = $stateParams.constraintsName;

        $scope.edit = function () {
            $modal.open({
                templateUrl: 'partials/modal/editConstraints.html',
                controller: 'ConstraintsEditController',
                size: 'lg',
                resolve: {
                    constraintsType: function () {
                        if (isSubscription($scope.constraintsName)) {
                            return 'subscription';
                        } else {
                            return 'topic';
                        }
                    },
                    constraintsName: function () {
                        return $scope.constraintsName;
                    },
                    operation: function () {
                        return 'EDIT';
                    }
                }
            }).result.then(function () {
                loadConstraints();
            });
        };

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

        var loadConstraints = function () {
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
        };

        loadConstraints();
    }]);

constraints.controller('ConstraintsEditController', ['ConstraintsRepository', '$scope', '$uibModalInstance', 'constraintsType', 'constraintsName',
    function (constraintsRepository, $scope, $modal, constraintsType, constraintsName) {
        $scope.constraintsType = constraintsType;
        $scope.constraintsName = constraintsName;
        $scope.consumersNumber = 1;

        $scope.save = function () {
            if ($scope.constraintsType === 'topic') {
                constraintsRepository.updateTopicConstraints({
                    topicName: $scope.constraintsName,
                    constraints: {
                        consumersNumber: $scope.consumersNumber
                    }
                }).then(function () {
                    $modal.close();
                });
            } else {
                constraintsRepository.updateSubscriptionConstraints({
                    subscriptionName: $scope.constraintsName,
                    constraints: {
                        consumersNumber: $scope.consumersNumber
                    }
                }).then(function () {
                    $modal.close();
                });
            }
        }
    }]);
