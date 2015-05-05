angular.module('claudeApp').
    directive('eventMin', ['$window', function ($window) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/events/eventMin.html',
            scope : true,
            link : function (scope, element) {
                function resizeElem () {
                    $(element).find('.img_min').css('height', ($(element).clientWidth * 0.376) / 2);
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
                        $(element).classList.remove('large-4');
                        $(element).classList.add('large-12');
                    }
                }
            }
        }

    }]);