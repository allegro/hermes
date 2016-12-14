var topics = angular.module('hermes.topic.factory', []);

topics.factory('TopicFactory', ['TOPIC_CONFIG',
    function(topicConfig) {
        return {
            create: function() {
                var defaults = {retentionTime: {duration: 1}, contentType: 'JSON', ack: 'LEADER', maxMessageSize: 10240};
                _.merge(defaults, topicConfig.defaults);

                return defaults;
            }
        };
    }]);
