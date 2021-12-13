var repository = angular.module('hermes.topic.repository', ['hermes.subscription.repository']);

repository.factory('TopicRepository', ['DiscoveryService', '$resource', '$location', 'SubscriptionRepository',
    function (discovery, $resource, $location, subscriptionRepository) {

        var repository = $resource(discovery.resolve('/topics/:name'), null, {update: {method: 'PUT'}});
        var previewRepository = $resource(discovery.resolve('/topics/:name/preview'), null);
        var blacklistRepository = $resource(discovery.resolve('/blacklist/topics/:name'), null,
            { blacklist: { method: 'POST', url: discovery.resolve('/blacklist/topics') } });
        var listing = $resource(discovery.resolve('/topics'));
        var topicUsersRepository = $resource(discovery.resolve('/topics/:name/clients'));

        return {
            list: listing.query,
            get: function (name) {
                return repository.get({name: name}).$promise;
            },
            add: function (topic, schema) {
                return repository.save({}, angular.extend({}, topic, {"schema": schema})).$promise;
            },
            remove: function (topic) {
                return repository.delete({name: topic.name}).$promise;
            },
            save: function (topic, schema) {
                return repository.update({name: topic.name}, angular.extend({}, topic, {"schema": schema})).$promise;
            },
            listSubscriptions: function (topicName) {
                return subscriptionRepository.list(topicName);
            },
            listSubscriptionsWithDetails: function (topicName) {
                return subscriptionRepository.list(topicName).$promise.then(function (subscriptions) {
                    return _.map(subscriptions, function (subscription) {
                        return {
                            name: subscription,
                            details: subscriptionRepository.get(topicName, subscription)
                        };
                    });
                });
            },
            preview: function (topicName) {
                return previewRepository.query({name: topicName}).$promise;
            },
            blacklistStatus: function (topicName) {
                return blacklistRepository.get({name: topicName}).$promise;
            },
            blacklist: function (topicName) {
                return blacklistRepository.blacklist([topicName]);
            },
            unblacklist: function (topicName) {
                return blacklistRepository.delete({name: topicName});
            },
            getTopicUsers: function (topicName) {
                return topicUsersRepository.get({name: topicName}).$promise;
            }
        };
    }]);

repository.factory('OfflineClientsRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var repository = $resource(discovery.resolve('/topics/:topic/offline-clients'));

        return {
            get: function (topic) {
                return repository.query({topic: topic}).$promise;
            }
        };
    }]);
