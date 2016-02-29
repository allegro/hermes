var stats = angular.module('hermes.stats', ['ngResource', 'hermes.search', 'hermes.topic.repository']);

stats.controller('StatsController', ['$scope', 'SearchRepository', 'TopicRepository', function($scope, searchRepository, topicRepository) {

    $scope.topics = {};
    $scope.subscriptions = {};

    topicRepository.list().$promise
    .then(function(list) {
        $scope.topics.total = list.length;
    })
    .then(function() {
        searchRepository.search('topic', {query: {ack: {eq: 'ALL'}}}).then(function(ackAllTopics) {
            $scope.topics.ackAll = {
                count: ackAllTopics.length,
                ratio: (ackAllTopics.length / $scope.topics.total) * 100
            };
        });
        searchRepository.search('topic', {query: {trackingEnabled: {eq: true}}}).then(function(trackedTopics) {
            $scope.topics.tracked = {
                count: trackedTopics.length,
                ratio: (trackedTopics.length / $scope.topics.total) * 100
            };
        });
        searchRepository.search('topic', {query: {contentType: {eq: 'AVRO'}}}).then(function(avroTopics) {
            $scope.topics.avro = {
                count: avroTopics.length,
                ratio: (avroTopics.length / $scope.topics.total) * 100
            };
        });
    });

    searchRepository.search('subscription', {query: {name: {like: '.*'}}}).then(function(list) {
        $scope.subscriptions.total = list.length;
    })
    .then(function() {
        searchRepository.search('subscription', {query: {trackingEnabled: {eq: true}}}).then(function(trackedTopics) {
            $scope.subscriptions.tracked = {
                count: trackedTopics.length,
                ratio: (trackedTopics.length / $scope.subscriptions.total) * 100
            };
        });
        searchRepository.search('subscription', {query: {contentType: {eq: 'AVRO'}}}).then(function(avroTopics) {
            $scope.subscriptions.avro = {
                count: avroTopics.length,
                ratio: (avroTopics.length / $scope.subscriptions.total) * 100
            };
        });
    });

}]);
