var directives = angular.module('hermes.directives', []);

directives.directive('endpoint', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            ctrl.$parsers.unshift(function (value) {
                ctrl.$setValidity('endpoint',hasValidProtocol(value));
                return value;
            });

            var hasValidProtocol = function (value) {
                var supportedProtocols = [
                    "http",
                    "service",
                    "jms",
                    "pubsub",
                    "hdfs",
                    "zmq",
                    "ems"
                ];

                for (var i = 0; i < supportedProtocols.length; i++) {
                    if (beginsWith(supportedProtocols[i] + "://", value)) return true;
                }

                return false;
            };

            var beginsWith = function (needle, haystack) {
                return (haystack.substr(0, needle.length) === needle);
            };
        }
    };

});