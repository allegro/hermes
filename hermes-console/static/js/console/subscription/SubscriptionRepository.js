var repository = angular.module('hermes.subscription.repository', []);

repository.factory('SubscriptionRepository', ['DiscoveryService', '$resource', function (discovery, $resource) {
        var repository = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName'), null, {update: {method: 'PUT'}});
        var state = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/state'), null, {update: {method: 'PUT'}});
        var lastUndelivered = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/undelivered/last'));
        var undelivered = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/undelivered'), {}, {
                query: {
                    method:'GET',
                    isArray:true,
                    transformResponse: function (data, header) {
                        var extended = angular.fromJson(data);
                        angular.forEach(extended, function(item, idx) {
                                extended[idx].isPreviewable = function() {
                                    return typeof this.offset !== 'undefined' && typeof this.partition !== 'undefined';
                                };
                        });
                        return extended;
                    }
                }
            });
        var metrics = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/metrics'));
        var listing = $resource(discovery.resolve('/topics/:topicName/subscriptions/'));
        var eventTrace = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/events/:eventId/trace'));
        var retransmission = $resource(discovery.resolve('/topics/:topicName/subscriptions/:subscriptionName/retransmission'), null, {save: {method: 'PUT'}});

        return {
            list: function (topicName) {
                return listing.query({topicName: topicName});
            },
            get: function (topicName, subscriptionName) {
                return repository.get({topicName: topicName, subscriptionName: subscriptionName});
            },
            add: function (topicName, subscription) {
                return listing.save({topicName: topicName}, subscription);
            },
            save: function (topicName, subscription) {
                return repository.update({topicName: topicName, subscriptionName: subscription.name}, subscription);
            },
            metrics: function (topicName, subscriptionName) {
                return metrics.get({topicName: topicName, subscriptionName: subscriptionName})
                    .$promise.then(function(metrics) {
                        return {
                            rate: parseFloat(metrics.rate),
                            delivered: metrics.delivered,
                            discarded: metrics.discarded,
                            lag: metrics.lag,
                            responses: {
                                '2xx': parseFloat(metrics.codes2xx),
                                '4xx': parseFloat(metrics.codes4xx),
                                '5xx': parseFloat(metrics.codes5xx),
                                errors: parseFloat(metrics.otherErrors),
                                timeouts: parseFloat(metrics.timeouts)
                            },
                        };
                    });
            },
            remove: function (topicName, subscriptionName) {
                return repository.remove({topicName: topicName, subscriptionName: subscriptionName});
            },
            suspend: function (topicName, subscriptionName) {
                return state.update({topicName: topicName, subscriptionName: subscriptionName}, '"SUSPENDED"');
            },
            activate: function (topicName, subscriptionName) {
                return state.update({topicName: topicName, subscriptionName: subscriptionName}, '"ACTIVE"');
            },
            lastUndelivered: function(topicName, subscriptionName) {
                return lastUndelivered.get({topicName: topicName, subscriptionName: subscriptionName});
            },
            undelivered: function(topicName, subscriptionName) {
                return undelivered.query({topicName: topicName, subscriptionName: subscriptionName});
            },
            eventTrace: function(topicName, subscriptionName, eventId) {
                return eventTrace.query({topicName: topicName, subscriptionName: subscriptionName, eventId: eventId});
            },
            retransmit: function (topicName, subscriptionName, fromDate) {
                return retransmission.save({topicName: topicName, subscriptionName: subscriptionName}, {retransmissionDate: fromDate});
            }
        };
}]);
