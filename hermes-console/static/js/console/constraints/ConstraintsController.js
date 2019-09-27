var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsController', ['ConstraintsRepository', '$scope', '$stateParams', '$location', '$uibModal',
    function (constraintsRepository, $scope, $stateParams, $location, $modal) {

        var isSubscription = function (constraints) {
            return constraints.subscriptionName !== undefined;
        };

        $scope.edit = function (constraints) {
            $modal.open({
                templateUrl: 'partials/modal/editConstraints.html',
                controller: 'ConstraintsEditController',
                size: 'lg',
                resolve: {
                    constraintsType: function () {
                        return isSubscription(constraints) ? 'subscription' : 'topic';
                    },
                    constraintsName: function () {
                        return isSubscription(constraints) ? constraints.subscriptionName : constraints.topicName;
                    },
                    consumersNumber: function () {
                        return constraints.consumersNumber;
                    }
                }
            }).result.then(function () {
                loadConstraints();
            });
        };

        var addConstraints = function (constraintsType) {
            $modal.open({
                templateUrl: 'partials/modal/addConstraints.html',
                controller: 'ConstraintsAddController',
                size: 'lg',
                resolve: {
                    constraintsType: function () {
                        return constraintsType;
                    }
                }
            }).result.then(function () {
                loadConstraints();
            });
        };

        $scope.addTopicConstraints = function () {
            addConstraints('topic');
        };

        $scope.addSubscriptionConstraints = function () {
            addConstraints('subscription');
        };

        var loadConstraints = function () {
            constraintsRepository.getWorkloadConstraints()
                .then(function (workloadConstraints) {
                    $scope.topicConstraints = workloadConstraints.topicConstraints;
                    $scope.subscriptionConstraints = workloadConstraints.subscriptionConstraints;
                });
        };

        loadConstraints();
    }]);

constraints.controller('ConstraintsAddController', ['ConstraintsRepository', '$scope', '$uibModalInstance', 'constraintsType',
    function (constraintsRepository, $scope, $modalInstance, constraintsType) {
        $scope.constraintsType = constraintsType;
        $scope.consumersNumber = 1;

        $scope.save = function () {
            if ($scope.constraintsType === 'topic') {
                constraintsRepository.updateTopicConstraints({
                    topicName: $scope.constraintsName,
                    constraints: {
                        consumersNumber: $scope.consumersNumber
                    }
                }).then(function () {
                    $modalInstance.close();
                });
            } else {
                constraintsRepository.updateSubscriptionConstraints({
                    subscriptionName: $scope.constraintsName,
                    constraints: {
                        consumersNumber: $scope.consumersNumber
                    }
                }).then(function () {
                    $modalInstance.close();
                });
            }
        }
    }]);

constraints.controller('ConstraintsEditController', ['ConstraintsRepository', '$scope', '$uibModal', '$uibModalInstance',
    'constraintsType', 'constraintsName', 'consumersNumber',
    function (constraintsRepository, $scope, $modal, $modalInstance, constraintsType, constraintsName, consumersNumber) {
        $scope.constraintsType = constraintsType;
        $scope.constraintsName = constraintsName;
        $scope.consumersNumber = consumersNumber;

        $scope.save = function () {
            if ($scope.constraintsType === 'topic') {
                constraintsRepository.updateTopicConstraints({
                    topicName: $scope.constraintsName,
                    constraints: {
                        consumersNumber: $scope.consumersNumber
                    }
                }).then(closeModal);
            } else {
                constraintsRepository.updateSubscriptionConstraints({
                    subscriptionName: $scope.constraintsName,
                    constraints: {
                        consumersNumber: $scope.consumersNumber
                    }
                }).then(closeModal);
            }
        };

        $scope.remove = function () {
            $modal.open({
                templateUrl: 'partials/modal/removeConstraints.html',
                controller: 'ConstraintsRemoveController',
                size: 'lg'
            }).result.then(acceptHandler, dismissHandler);
        };

        var closeModal = function () {
            $modalInstance.close();
        };

        var acceptHandler = function (response) {
            if (response === 'REMOVE') {
                if ($scope.constraintsType === 'topic') {
                    constraintsRepository.removeTopicConstraints($scope.constraintsName)
                        .$promise
                        .then(closeModal);
                } else {
                    var splittedName = $scope.constraintsName.split("$");
                    var topicName = splittedName[0];
                    var subscriptionName = splittedName[1];
                    constraintsRepository.removeSubscriptionConstraints(topicName, subscriptionName)
                        .$promise
                        .then(closeModal);
                }
            }
        };

        var dismissHandler = function () {
            $modalInstance.dismiss();
        };
    }]);

constraints.controller('ConstraintsRemoveController', ['ConstraintsRepository', '$scope', '$uibModalInstance',
    function (constraintsRepository, $scope, $modalInstance) {
        $scope.remove = function () {
            $modalInstance.close('REMOVE')
        };
    }]);
