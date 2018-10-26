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
                    contentType: 'JSON',
                    subscriptionPolicy: {
                        messageTtl: 3600,
                        messageBackoff: 100,
                        sendingDelay: 0
                    },
                    monitoringDetails: {
                        severity: 'NON_IMPORTANT',
                        reaction: ''
                    },
                    state: 'ACTIVE' // must be in sync with editSubscription.html:255
                };

                _.merge(defaults, subscriptionConfig.defaults);
                return defaults;
            }
        };

    }]);
