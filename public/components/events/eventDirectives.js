angular.module('claudeApp').
    directive('eventMin', ['$window', function ($window) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/events/eventMin.html',
            scope : true,
            link : function (scope, element) {
                function resizeElem () {
                    var waitForElem = setInterval(function () {
                        if ($(element).innerWidth() > 50) {
                            clearInterval(waitForElem);
                            $(element).find('.img_min').css('height', Math.ceil($(element).innerWidth() * 0.376));
                        }
                    }, 100);
                    waitForElem;
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
    directive('eventInfo', [ '$rootScope', function ($rootScope) {
        return {
            restrict : 'C',
            link : function (element) {
                if ( $rootScope.window != 'small' && $rootScope.window != 'medium') {
                    if ($(element).offsetLeft < 30) {
                        $(element).removeClass('large-4');
                        $(element).addClass('large-12');
                    }
                }
            }
        }

    }]);