angular.module('hermes.filters')
    .controller('HttpHeaderFiltersEditorController', ['$scope',
        function ($scope) {

            $scope.addFilter = function () {
                if (!$scope.filter.header || !$scope.filter.matcher) {
                    return;
                }

                $scope.filters.push({
                    type: 'header',
                    header: $scope.filter.header,
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

        }])
    .directive('httpHeaderFiltersEditor', function () {
        return {
            controller: 'HttpHeaderFiltersEditorController',
            restrict: 'E',
            templateUrl: 'partials/filter/httpHeaderFiltersEditor.html',
            scope: {
                form: '=',
                filters: '=filters'
            }
        };
    });
