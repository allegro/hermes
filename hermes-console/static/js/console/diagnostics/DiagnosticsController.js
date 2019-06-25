var diagnostics = angular.module('hermes.diagnostics', [
    'ui.bootstrap',
    'hermes.diagnostics.repository'
]);

diagnostics.controller('DiagnosticsController', ['DiagnosticsRepository', '$scope', '$stateParams',
    function (diagnosticsRepository, $scope, $stateParams) {
        $scope.groupName = $stateParams.groupName;
        $scope.topicName = $stateParams.topicName;
        $scope.subscriptionName = $stateParams.subscriptionName;

        diagnosticsRepository.getConsumerGroups($scope.topicName, $scope.subscriptionName)
            .then(function (consumerGroups) {
                $scope.consumerGroups = consumerGroups;
            })
            .catch(function () {
                $scope.consumerGroups = [];
            });

        $scope.isConsumerGroupStable = function (consumerGroup) {
            return consumerGroup.state === 'Stable';
        };

        $scope.isConsumerGroupDuringRebalance = function (consumerGroup) {
            return consumerGroup.state === 'PreparingRebalance' || consumerGroup.state === 'CompletingRebalance';
        };

        $scope.isConsumerGroupUnstable = function (consumerGroup) {
            return !$scope.isConsumerGroupStable(consumerGroup) && !$scope.isConsumerGroupDuringRebalance(consumerGroup);
        };

        $scope.isJsonTopic = function (partition) {
            return partition.contentType === 'JSON';
        };

        $scope.isAvroTopic = function (partition) {
            return partition.contentType === 'AVRO';
        };

        $scope.consumerGroupExists = function () {
            return !_.isEmpty($scope.consumerGroups);
        };

        $scope.copyTopicNameToClipboard = function (partition) {
            var tempElement = document.createElement('textarea');
            tempElement.value = partition.topic;
            document.body.appendChild(tempElement);
            tempElement.select();
            document.execCommand('copy');
            document.body.removeChild(tempElement);
        };
    }]);
