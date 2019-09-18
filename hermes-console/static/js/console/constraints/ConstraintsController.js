var constraints = angular.module('hermes.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('ConstraintsController', ['ConstraintsRepository', '$scope',
    function (constraintsRepository, $scope) {
        $scope.hello = 'Hello constraints controller!';

        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                $scope.topicConstraints = [];
                for (topicName in workloadConstraints.topicConstraints) {
                    if (workloadConstraints.topicConstraints.hasOwnProperty(topicName)) {
                        $scope.topicConstraints.push({
                            topicName: topicName,
                            consumersNumber: workloadConstraints.topicConstraints[topicName].consumersNumber
                        });
                    }
                }
                console.log($scope.topicConstraints);

                $scope.subscriptionConstraints = [];
                for (subscriptionName in workloadConstraints.subscriptionConstraints) {
                    if (workloadConstraints.subscriptionConstraints.hasOwnProperty(subscriptionName)) {
                        $scope.subscriptionConstraints.push({
                            subscriptionName: subscriptionName,
                            consumersNumber: workloadConstraints.subscriptionConstraints[subscriptionName].consumersNumber
                        });
                    }
                }
                console.log($scope.subscriptionConstraints);
            })
            .catch(function () {
                $scope.workloadConstraints = {};
            })
    }]);