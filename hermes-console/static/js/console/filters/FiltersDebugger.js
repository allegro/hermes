angular.module('hermes.filters.debugger', ['hermes.filters.repository'])
    .controller('FiltersDebuggerController', ['$scope', '$uibModalInstance', 'FiltersRepository', 'topicName',
        'messageFilters', 'topicContentType',
        function ($scope, $modal, filtersRepository, topicName, messageFilters, topicContentType) {

            $scope.topicName = topicName;
            $scope.messageFilters = messageFilters;
            $scope.topicContentType = topicContentType;

            resetVerificationState();

            $scope.verify = function () {
                resetVerificationState();
                var filtersWithoutHeaderFilterType = _.reject($scope.messageFilters, function (f) {
                    return f.header;
                });
                filtersRepository.verify($scope.topicName, filtersWithoutHeaderFilterType, $scope.message).$promise
                    .then(function (response) {
                        $scope.verificationStatus = response.status;
                        $scope.errorMessage = response.errorMessage;
                    })
                    .catch(function (response) {
                        $scope.verificationStatus = 'ERROR';
                        $scope.errorMessage = response.data.message;
                    })
                    .finally(function () {
                        $scope.verificationInProgress = false;
                    });
                beautifyText();
            };

            $scope.updateFilters = function () {
                $modal.close({messageFilters: $scope.messageFilters});
            };

            function resetVerificationState() {
                $scope.verificationStatus = '';
                $scope.errorMessage = null;
                $scope.verificationInProgress = false;
            }
            function beautifyText(){
                const obj_message = JSON.parse($scope.message);
                if (obj_message !== undefined) {
                    $scope.message = JSON.stringify(obj_message, null, 4);
                }
            }
        }])
    .factory('FiltersDebuggerModalFactory', ['$uibModal', function ($modal) {
        return {
            open: function (topicName, messageFilters, topicContentType) {
                return $modal.open({
                    templateUrl: 'partials/modal/debugFilters.html',
                    controller: 'FiltersDebuggerController',
                    size: 'lg',
                    backdrop: 'static',
                    resolve: {
                        topicName: function () {
                            return topicName;
                        },
                        topicContentType: function () {
                            return topicContentType;
                        },
                        messageFilters: function () {
                            return _.cloneDeep(messageFilters);
                        }
                    }
                }).result;
            }
        };
    }]);
