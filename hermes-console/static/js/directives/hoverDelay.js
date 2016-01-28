var directives = angular.module('hermes.directives', []);

directives.directive('hoverDelay', ['$timeout', function($timeout) {

    return {
        link: function(scope, element, attrs) {
            var counting = false;
            var alreadyRun = false;
            var delay = parseInt(attrs.hoverDelayTime, 10) || 1000;
            var repeat = attrs.hoverDelayRepeat || false;

            element.on('mouseover', function() {
                if(!counting && (repeat || !alreadyRun)) {
                    counting = true;
                    $timeout(function() {
                        if(counting) {
                            scope.$eval(attrs.hoverDelay);
                            alreadyRun = true;
                        }
                        counting = false;
                    }, delay);
                }
            });

            element.on('mouseleave', function() {
                counting = false;
            });
        }
    };
}]);
