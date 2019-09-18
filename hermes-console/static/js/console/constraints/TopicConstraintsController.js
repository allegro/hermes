var constraints = angular.module('hermes.topic.constraints', [
    'ui.bootstrap',
    'hermes.constraints.repository'
]);

constraints.controller('TopicConstraintsController', ['ConstraintsRepository', '$scope', '$stateParams',
    function (constraintsRepository, $scope, $stateParams) {
        constraintsRepository.getWorkloadConstraints()
            .then(function (workloadConstraints) {
                $scope.topicConstraints = workloadConstraints.topicConstraints
                    .find(function (topicCons) {
                        return topicCons.topicName === topicName;
                    });
            });
    }]);
