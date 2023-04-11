var subscriptionOffsets = angular.module('hermes.subscriptionOffsets', ['hermes.subscriptionOffsets.service']);

subscriptionOffsets.controller('SubscriptionOffsetsController', ['$scope', 'SubscriptionService', 'toaster',
    function ($scope, subscriptionService, toaster) {
        $scope.error = null;
        $scope.subscriptionFullName = '';

        $scope.moveOffsets = function () {
            var subscription = parseSubscription($scope.subscriptionFullName);
            subscriptionService.moveOffsets(subscription.topicName, subscription.subscriptionName)
                .then(function () {
                    clearError();
                    clearData();
                    displaySuccess();
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

        function displaySuccess() {
            toaster.pop('success', 'Success', 'Offsets moved successfully');
        }

        function clearData() {
            $scope.subscriptionFullName = "";
        }

        function parseSubscription(subscriptionFullName) {
            var subscriptionChunks = subscriptionFullName.split('.');
            var subscriptionName = subscriptionChunks.pop();
            return {
                topicName: subscriptionChunks.join('.'),
                subscriptionName: subscriptionName,
            };
        }

        function validate() {

        }
    }]);