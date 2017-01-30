angular.module('hermes.owner')
    .controller('ctrl', ['$scope', 'OwnerRepository', function ($scope, ownerRepository) {

        ownerRepository.getSourceNames().then(function (sources) {
            $scope.possibleSources = sources;
            $scope.sourceSelectModel = _.find(sources, function(s) { return s.name == $scope.sourceName}) || sources[0];
            if ($scope.ownerId) {
                ownerRepository.getOwner($scope.sourceSelectModel.name, $scope.ownerId).then(function(owner) {
                    if ($scope.sourceSelectModel.autocomplete) {
                        $scope.ownerInputModel = {id: owner.id, name: owner.name};
                    } else {
                        $scope.ownerInputModel = owner.id;
                    }
                });
            }
        });

        $scope.onSourceChanged = function () {
            $scope.ownerInputModel = '';
        };

        $scope.$watch('sourceSelectModel', function (model) {
            if (model === undefined) {
                return;
            }

            $scope.sourceName= model.name;
        });

        $scope.$watch('ownerInputModel', function(model) {
            if (model === undefined) {
                return;
            }

            if (model instanceof Object && model.id !== undefined) {
                $scope.ownerId= model.id;
            } else {
                $scope.ownerId= model;
            }
        });

        $scope.owners = function(searchString) {
            return ownerRepository.getOwners($scope.sourceSelectModel.name, searchString);
        };
    }])
    .directive('ownerSelector', function () {
        return {
            controller: 'ctrl',
            restrict: 'E',
            templateUrl: 'partials/ownerSelector.html',
            scope: {
                sourceName: '=source',
                ownerId: '=owner',
                form: '='
            }
        };
    });
