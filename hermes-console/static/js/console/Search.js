var search = angular.module('hermes.search', []);

search.controller('SearchController', ['$scope', '$stateParams', 'SearchRepository',
    function($scope, $stateParams, searchRepository) {

    $scope.entity = $stateParams.entity || 'subscription';
    $scope.property = $stateParams.property || 'endpoint';
    $scope.operator = $stateParams.operator || 'like';
    $scope.pattern = $stateParams.pattern || '';

    $scope.fetching = false;
    $scope.state = {
        notSearched: true,
        noResults: false
    };

    $scope.search = function() {
        $scope.fetching = true;
        var query = createQuery($scope.property, $scope.operator, $scope.pattern);

        searchRepository.search($scope.entity, query).then(function (items) {
            $scope.items = postProcess($scope.entity, items);
            $scope.fetching = false;
            $scope.state.notSearched = false;
            $scope.state.noResults = !items || items.length === 0;
        });
    };

    function createQuery(property, operator, value) {
        var query = {};
        query[property] = {};

        var sanitizedOperator = operator || 'like';
        query[property][sanitizedOperator] = decorateQueryValue(value);

        return {query: query};
    }

    function decorateQueryValue(value) {
        if (value === '' || value.length < 4) {
            return value;
        }

        if (value.startsWith('.*') || value.endsWith('.*')) {
            return value;
        }

        return '.*' + value + '.*';
    }

    function postProcess(entity, items) {
        if (entity === 'topic') {
            return _.map(items, function(item) {
                var fullName = decomposeTopicName(item.name);
                return {
                    name: item.name,
                    data: [{label: 'owner', value: item.owner.id}],
                    url: '#/groups/' + fullName.group + '/topics/' + item.name
                };
            });
        }
        else if (entity === 'subscription') {
            return _.map(items, function(item) {
                var fullName = decomposeTopicName(item.topicName);
                return {
                    name: item.topicName + '.' + item.name,
                    data: [{label: 'endpoint', value: item.endpoint}, {label: 'owner', value: item.owner.id}, {label: 'status', value: item.state}],
                    url: '#/groups/' + fullName.group + '/topics/' + item.topicName + '/subscriptions/' + item.name
                };
            });
        }
    }

    function decomposeTopicName(topicName) {
        return {
            group: topicName.substring(0, topicName.lastIndexOf(".")),
            topic: topicName.substring(topicName.lastIndexOf(".") + 1)
        };
    }
}]);

search.factory('SearchRepository', ['$resource', 'DiscoveryService',
    function ($resource, discovery) {
        var querySubscriptions = $resource(discovery.resolve('/query/subscriptions'), null, {query: {method: 'POST', isArray: true}});
        var queryTopics = $resource(discovery.resolve('/query/topics'), null, {query: {method: 'POST', isArray: true}});

        return {
            search: function (entity, query) {
                if (entity === 'topic') {
                    return queryTopics.query(query).$promise;
                }
                else if (entity === 'subscription') {
                    return querySubscriptions.query(query).$promise;
                }
            }
        };
    }]);
