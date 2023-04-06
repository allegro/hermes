var subscriptionOffsets = angular.module('hermes.subscriptionOffsets', ['hermes.subscriptionOffsets.service']);

subscriptionOffsets.controller('SubscriptionOffsetsController', ['$scope', 'SubscriptionService',
    function ($scope, subscriptionService) {
        $scope.error = null;
        $scope.subscriptionFullName = '';

        $scope.moveOffsets = function () {
            var subscription = parseSubscription($scope.subscriptionFullName);
            subscriptionService.moveOffsets(subscription.topicName, subscription.subscriptionName)
                .then(function () {
                    clearError();
                })
                .catch(function (e) {
                    displayError(e);
                });
        };

        function displayError(msg) {
            $scope.error = msg;
        }

        function clearError() {
            $scope.error = null;
        }

        function parseSubscription(subscriptionFullName) {
            var subscriptionChunks = subscriptionFullName.split('.');
            var subscriptionName = subscriptionChunks.pop();
            return {
                topicName: subscriptionChunks.join('.'), subscriptionName: subscriptionName
            };
        }

        function validate() {

        }
    }]);