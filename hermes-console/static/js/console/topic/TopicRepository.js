var repository = angular.module('hermes.topic.repository', ['hermes.subscription.repository']);

repository.factory('TopicRepository', ['DiscoveryService', '$resource', '$location', 'SubscriptionRepository',
    'SchemaRepository', 'TOPIC_CONFIG',
    function (discovery, $resource, $location, subscriptionRepository, schemaRepository, topicConfig) {

        var repository = $resource(discovery.resolve('/topics/:name'), null, {update: {method: 'PUT'}});
        var previewRepository = $resource(discovery.resolve('/topics/:name/preview'), null);
        var blacklistRepository = $resource(discovery.resolve('/blacklist/topics/:name'), null,
            { blacklist: { method: 'POST', url: discovery.resolve('/blacklist/topics') } });
        var listing = $resource(discovery.resolve('/topics'));

        function wrapSchemaSave(topic, schema, promise) {
            if (schema && schema.trim()) {
                return promise.then(function () {
                    return schemaRepository.save(topic.name, schema).$promise;
                });
            }
            return promise;
        }

        function wrapSchemaRemove(topic, promise) {
            if (topicConfig.removeSchema && topic.contentType == 'AVRO') {
                return promise.then(function () {
                    return schemaRepository.remove(topic.name).$promise;
                });
            }
            return promise;
        }

        function ngPromiseCleaner(key, value) {
            if (_.contains(["$promise", "$resolved"], key)) {
                return undefined;
            }
            return value;
        }

        return {
            list: listing.query,
            get: function (name) {
                return repository.get({name: name}).$promise.then(function(topic) {
                    return schemaRepository.get(topic.name).$promise.then(function(schema) {
                        var schemaStr = JSON.stringify(schema, ngPromiseCleaner, 2);

                        topic.shortName = topic.name.substring(topic.name.lastIndexOf('.') + 1);

                        return {topic: topic, messageSchema: schemaStr != '{}' ? schemaStr : null};
                    })
                });
            },
            add: function (topic, schema) {
                return wrapSchemaSave(topic, schema, listing.save({}, topic).$promise);
            },
            remove: function (topic) {
                return wrapSchemaRemove(topic, repository.delete({name: topic.name}).$promise);
            },
            save: function (topic, schema) {
                return wrapSchemaSave(topic, schema, repository.update({name: topic.name}, topic).$promise);
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
                return blacklistRepository.blacklist([topicName])
            },
            unblacklist: function (topicName) {
                return blacklistRepository.delete({name: topicName})
            }
        };
    }]);

repository.factory('SchemaRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var repository = $resource(discovery.resolve('/topics/:name/schema'));

        return {
            get: function (name) {
                return repository.get({name: name});
            },
            save: function (name, schema) {
                return repository.save({name: name}, schema);
            },
            remove: function(name) {
                return repository.remove({name: name});
            }
        };
    }]);
