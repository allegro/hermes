var search = angular.module('hermes.search', []);

search.controller('SearchController', ['SearchRepository', '$scope', function(searchRepository, $scope) {

    $scope.fetching = false;

    $scope.searchSubscriptions = function () {
        $scope.fetching = true;
        searchRepository.searchSubscriptions($scope.endpoint).then(function (subscriptions) {
            $scope.subscriptions = subscriptions;
            $scope.fetching = false;
        });
    };

    $scope.getGroup = function(topicName) {
        return topicName.substring(0, topicName.lastIndexOf("."));
    };

}]);

search.factory('SearchRepository', ['$resource', 'DiscoveryService',
    function ($resource, discovery) {
        var querySubscriptions = $resource(discovery.resolve('/query/subscription'), null, {query: {method: 'POST', isArray: true}});

        var endpointQuery = function(endpoint) {
            return { query: {
                    endpoint: {
                        like: endpoint
                    }
                }
            }
        };

        return {
            searchSubscriptions: function (endpoint) {
                return querySubscriptions.query(endpointQuery(endpoint)).$promise;
            }
        };
    }]);
