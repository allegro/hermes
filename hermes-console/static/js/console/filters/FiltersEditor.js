angular.module('hermes.filters')
    .controller('FiltersEditorController', ['$scope', '$uibModal',
        'FiltersDebuggerModalFactory',
        function ($scope, $modal, filtersDebuggerModal) {

            $scope.addFilter = function () {
                if (!$scope.filter.path || !$scope.filter.matcher) {
                    return;
                }

                $scope.filters.push({
                    type: $scope.topicContentType === 'JSON' ? 'jsonpath' : 'avropath',
                    path: $scope.filter.path,
                    matcher: $scope.filter.matcher,
                    matchingStrategy: $scope.filter.matchingStrategy
                });
                $scope.filter = {};
            };

            $scope.delFilter = function (filter) {
                $scope.filters = _.reject($scope.filters, function (f) {
                    return f === filter;
                });
            };

            $scope.debugFilters = function () {
                filtersDebuggerModal.open($scope.topicName, $scope.filters, $scope.topicContentType)
                    .then(function (result) {
                        $scope.filters = result.messageFilters;
                    });
            };
        }])
    .directive('filtersEditor', function () {
        return {
            controller: 'FiltersEditorController',
            restrict: 'E',
            templateUrl: 'partials/filter/filtersEditor.html',
            scope: {
                form: '=',
                topicContentType: '=topicContentType',
                filters: '=filters',
                withDebugger: '=',
                topicName: '='
            }
        };
    });
