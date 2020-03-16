var metricStore = angular.module('hermes.metrics', []);

metricStore.factory('MetricStoreUrlResolver', ['METRICS_CONFIG', function(config) {
        if(config.type == 'graphite') {
            return new GraphiteUrlResolver(config.graphite);
        } else {
            console.error('Metric store type: ' + config.type + ' is not supported.');
        }
    }]);

function GraphiteUrlResolver(config) {
    var prefix = config.prefix;

    function compile(template, values) {
        var regex = /{}/g;
        var text = template;
        var i = 0;

        while(regex.test(text)) {
            text = text.replace(/{}/, values[i]);
            i++;
        }
        return text;
    }

    function sanitize(value) {
        return value.replace(/\./g, '_');
    }

    function consumerGroupId(group, topic, subscription) {
        return compile("{}_{}_{}", _.map([group, topic, subscription], function(item) {
            return sanitize(item.replace(/_/g, '__'));
        }));
    }

    function url(value) {
        return config.url + '/render/?width=1514&height=952&target=' + value;
    }

    function topicPath(group, topic) {
        return sanitize(group) + '.' + sanitize(topic);
    }

    function subscriptionPath(group, topic, subscription) {
        return topicPath(group, topic) + '.' + sanitize(subscription);
    }

    this.topicMetrics = function(group, topic) { return {
            rate: url(compile('sumSeries({}.producer.*.meter.{}.m1_rate)', [prefix, topicPath(group, topic)])),
            deliveryRate: url(compile('sumSeries({}.consumer.*.meter.{}.m1_rate)', [prefix, topicPath(group, topic)])),
            published: url(compile('sumSeries({}.producer.*.published.{}.count)', [prefix, topicPath(group, topic)])),
            latency: url(compile('{}.producer.*.ack-*.latency.{}.p99', [prefix, topicPath(group, topic)])),
            messageSize: url(compile('{}.producer.*.message-size.{}.max', [prefix, topicPath(group, topic)]))
        };
    };

    this.subscriptionMetrics = function(group, topic, subscription) { return {
            rate: url(compile('sumSeries({}.consumer.*.meter.{}.m1_rate)', [prefix, subscriptionPath(group, topic, subscription)])),
            delivered: url(compile('sumSeries({}.consumer.*.delivered.{}.count)', [prefix, subscriptionPath(group, topic, subscription)])),
            discarded: url(compile('sumSeries({}.consumer.*.discarded.{}.count)', [prefix, subscriptionPath(group, topic, subscription)])),
            outputRate: url(compile('sumSeries({}.consumer.*.output-rate.{})', [prefix, subscriptionPath(group, topic, subscription)])),
            latency: url(compile('{}.consumer.*.latency.{}.p99', [prefix, subscriptionPath(group, topic, subscription)])),
            timeouts: url(compile('sumSeries({}.consumer.*.status.{}.errors.timeout.m1_rate)', [prefix, subscriptionPath(group, topic, subscription)])),
            networkErrors: url(compile('sumSeries({}.consumer.*.status.{}.errors.other.m1_rate)', [prefix, subscriptionPath(group, topic, subscription)])),
            '2xx': url(compile('sumSeries({}.consumer.*.status.{}.2xx.m1_rate)', [prefix, subscriptionPath(group, topic, subscription)])),
            '4xx': url(compile('sumSeries({}.consumer.*.status.{}.4xx.m1_rate)', [prefix, subscriptionPath(group, topic, subscription)])),
            '5xx': url(compile('sumSeries({}.consumer.*.status.{}.5xx.m1_rate)', [prefix, subscriptionPath(group, topic, subscription)])),
            lag: url(compile('sumSeries({}.consumer-offset.*.{}_{}*.{}.*.lag)', [
                    prefix, sanitize(group), sanitize(topic), consumerGroupId(group, topic, subscription)
                ]))
        };
    };
}
