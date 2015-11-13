angular.module('claudeApp').
    directive('eventMin', ['$window', '$compile', function ($window) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/events/eventMin.html',
            controller: 'EventMinCtrl',
            scope : true,
            link : function (scope, element) {
                function resizeElem () {
                    var waitForElem = setInterval(function () {
                        if ($(element).innerWidth() > 50) {
                            clearInterval(waitForElem);
                            $(element).find('.img_min').css('height', Math.round($(element).innerWidth() * 0.35));
                        }
                    }, 100);
                }
                resizeElem();
                $window.addEventListener('resize', resizeElem);
                scope.$on('$destroy', function () {
                    $window.removeEventListener('resize', resizeElem);
                })
            }
        }
    }]);

angular.module('claudeApp').
    directive('eventInfo', [ '$rootScope', '$compile', function ($rootScope, $compile) {
        return {
            restrict : 'C',
            link : function (scope, element) {
                if ( $rootScope.window != 'small' && $rootScope.window != 'medium') {
                    var waitForBinding = setInterval(function () {
                        if ($(document).find('#description').html().length > 0) {
                            clearInterval(waitForBinding);
                            if ($(element).offset().left < 30) {
                                $(element).removeClass('large-4');
                                $(element).addClass('large-12');
                                $compile(element.contents())(scope);

                            }
                        }
                    }, 500)
                }
            }
        };
    }]);