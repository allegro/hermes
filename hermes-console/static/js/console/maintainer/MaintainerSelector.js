angular.module('hermes.maintainer')
    .controller('ctrl', ['$scope', 'MaintainerService', function ($scope, maintainerService) {

        maintainerService.getSourceNames().then(function (sources) {
            $scope.possibleSources = sources;
            $scope.sourceSelectModel = _.find(sources, function(s) { return s.name == $scope.sourceName}) || sources[0];
            if ($scope.maintainerId) {
                maintainerService.getMaintainer($scope.sourceSelectModel.name, $scope.maintainerId).then(function(maintainer) {
                    if ($scope.sourceSelectModel.hinting) {
                        $scope.maintainerInputModel = {id: maintainer.id, name: maintainer.name};
                    } else {
                        $scope.maintainerInputModel = maintainer.id;
                    }
                });
            }
        });

        $scope.onSourceChanged = function () {
            $scope.maintainerInputModel = '';
        };

        $scope.$watch('sourceSelectModel', function (model) {
            if (model === undefined) {
                return;
            }

            $scope.sourceName= model.name;
        });

        $scope.$watch('maintainerInputModel', function(model) {
            if (model === undefined) {
                return;
            }

            if (model instanceof Object && model.id !== undefined) {
                $scope.maintainerId= model.id;
            } else {
                $scope.maintainerId= model;
            }
        });

        $scope.maintainers = function(searchString) {
            return maintainerService.getMaintainers($scope.sourceSelectModel.name, searchString);
        };
    }])
    .directive('maintainerSelector', function () {
        return {
            controller: 'ctrl',
            restrict: 'E',
            templateUrl: 'partials/maintainerSelector.html',
            scope: {
                sourceName: '=source',
                maintainerId: '=maintainer',
                form: '='
            }
        };
    });
