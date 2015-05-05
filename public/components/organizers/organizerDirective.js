angular.module('claudeApp').
    directive('organizerMin', ['$window', '$rootScope',
        function ($window, $rootScope) {
            return {
                restrict : 'E',
                templateUrl: 'assets/components/organizers/organizerMin.html',
                scope : true,
                link : function (scope, element) {
                    function resizeElem () {
                        if ($rootScope.window != 'small') {
                            $(element).Height = 94 + (($(element).clientWidth * 0.376) / 2);
                        }
                    }
                    resizeElem();
                    $window.addEventListener('resize', resizeElem);
                    scope.$on('$destroy', function () {
                        $window.removeEventListener('resize', resizeElem);
                    })
                }
            }
        }]);