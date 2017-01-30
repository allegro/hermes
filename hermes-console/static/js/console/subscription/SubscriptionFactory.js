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
                    contentType: 'JSON',
                    subscriptionPolicy: {
                        messageTtl: 3600,
                        messageBackoff: 100,
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
