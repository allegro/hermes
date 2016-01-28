var health = angular.module('hermes.subscription.health', ['hermes.subscription.repository']);

health.factory('SubscriptionHealth', ['SubscriptionRepository', function (SubscriptionRepository) {

        function isLagging(topicMetrics, subscriptionMetrics) {
            if (topicMetrics.rate !== null && topicMetrics.rate > 0) {
                var lagInSeconds = subscriptionMetrics.lag / topicMetrics.rate;
                return lagInSeconds > 600;
            }
            return false;
        }

        function isMalfunctioning(subscriptionMetrics) {
            return subscriptionMetrics.responses['5xx'] > 0.1 * subscriptionMetrics.rate;
        }

        function isSlowResponses(subscriptionMetrics) {
            return subscriptionMetrics.responses.timeouts > 0.1 * subscriptionMetrics.rate;
        }

        function isUnreachable(subscriptionMetrics) {
            return subscriptionMetrics.responses.errors > 0.5 * subscriptionMetrics.rate;
        }

        function isMalformedEvents(subscription, subscriptionMetrics) {
            return subscription.subscriptionPolicy.retryClientErrors &&
                subscriptionMetrics.responses['4xx'] > 0.1 * subscriptionMetrics.rate;
        }

        function isSlowReceiver(topicMetrics, subscriptionMetrics) {
            return subscriptionMetrics.rate < 0.8 * topicMetrics.rate;
        }

        return {
            healthStatus: function(subscription, topicMetrics, subscriptionMetrics) {
                var status = {
                    lagging: isLagging(topicMetrics, subscriptionMetrics),
                    slowReceiver: isSlowReceiver(topicMetrics, subscriptionMetrics),
                    malfunctioning: isMalfunctioning(subscriptionMetrics),
                    malformedEvents: isMalformedEvents(subscription, subscriptionMetrics),
                    slow: isSlowResponses(subscriptionMetrics),
                    unreachable: isUnreachable(subscriptionMetrics),
                };
                var healthy = !_.some(status, function(v) { return v; });
                status.healthy = healthy || subscription.state == 'SUSPENDED';

                return status;
            }
        };
    }]);
