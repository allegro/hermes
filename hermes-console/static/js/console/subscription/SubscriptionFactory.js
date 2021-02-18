var subscriptions = angular.module('hermes.subscription.factory', []);

subscriptions.factory('SubscriptionFactory', ['SUBSCRIPTION_CONFIG', function (subscriptionConfig) {
        return {
            create: function (topicName) {
                var defaults = {
                    topicName: topicName,
                    name: '',
                    endpoint: '',
                    description: '',
                    owner: {
                        source: '',
                        id: ''
                    },
                    deliveryType: 'SERIAL',
                    mode: 'ANYCAST',
                    filters: [],
                    headers: [],
                    contentType: 'JSON',
                    subscriptionPolicy: {
                        messageTtl: 3600,
                        messageBackoff: 1000,
                        sendingDelay: 0,
                        backoffMultiplier: 1.0,
                        backoffMaxIntervalInSec: 600
                    },
                    monitoringDetails: {
                        severity: 'NON_IMPORTANT',
                        reaction: ''
                    }
                };

                _.merge(defaults, subscriptionConfig.defaults);
                return defaults;
            }
        };

    }]);
