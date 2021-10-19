var owner = angular.module('hermes.owner', []);
owner.controller('OwnerNameController', ['$scope', 'OwnerRepository', function ($scope, ownerRepository) {
    $scope.$watch('owner', function (newOwner) {
        if (newOwner !== undefined && newOwner.id && newOwner.source) {
            ownerRepository.getOwner(newOwner.source, newOwner.id).then(function (owner) {
                $scope.name = owner.name;
            });
        } else {
            $scope.name = "";
        }
    }, true);
}]);
owner.directive('ownerName', function () {
    return {
        controller: 'OwnerNameController',
        restrict: 'E',
        template: '{{name}}',
        scope: {
            owner: '='
        }
    };
});