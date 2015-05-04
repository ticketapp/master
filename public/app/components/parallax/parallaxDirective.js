angular.module('claudeApp').
    directive('parallaxContent', ['$rootScope', '$window', 'DetectPath',
        function ($rootScope, $window, DetectPath) {
        return {
            restrict : 'C',
            scope : true,
            link : function (scope, element) {
                function resizeEl() {
                    if ($rootScope.path != 'home') {
                        $(element).css('margin-top', window.innerWidth * 0.376 + 'px');
                    } else {
                        $(element).css('margin-top', '500px');
                    }
                }

                resizeEl();
                $window.addEventListener('resize', resizeEl);
                scope.$on('$destroy', function () {
                    $window.removeEventListener('resize', resizeEl);
                })
            }
        }
    }]);