angular.module('hermes.owner')
    .controller('OwnerSelectorController', ['$scope', 'OWNER_CONFIG', 'OwnerRepository',
        function ($scope, config, ownerRepository) {

        ownerRepository.getSourceNames().then(function (sources) {
            $scope.possibleSources = _.filter(sources, function (source) {
                return !source.deprecated || $scope.sourceName === source.name;
            });
            $scope.sourceSelectModel = _.find($scope.possibleSources, {'name': $scope.sourceName}) || _.first($scope.possibleSources);
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

            $scope.sourceName = model.name;
        });

        $scope.$watch('ownerInputModel', function(model) {
            if (model === undefined) {
                return;
            }

            if (model instanceof Object && model.id !== undefined) {
                $scope.ownerId = model.id;
            } else {
                $scope.ownerId = model;
            }
        });

        $scope.owners = function(searchString) {
            return ownerRepository.getOwners($scope.sourceSelectModel.name, searchString);
        };

        $scope.placeholder = function () {
            if (!$scope.sourceName) {
                return '';
            }

            var sourceConfig = _.find(config.sources, function (s) {
                return s.name == $scope.sourceName
            });

            if (sourceConfig && sourceConfig.placeholder) {
                return sourceConfig.placeholder;
            }

            return "who's the owner?";
        }
    }])
    .directive('ownerSelector', function () {
        return {
            controller: 'OwnerSelectorController',
            restrict: 'E',
            templateUrl: 'partials/ownerSelector.html',
            scope: {
                sourceName: '=source',
                ownerId: '=owner',
                form: '='
            }
        };
    });
