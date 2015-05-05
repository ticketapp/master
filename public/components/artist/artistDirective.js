angular.module('claudeApp').
    directive('artistMin', ['$window', '$rootScope',
        function ($window, $rootScope) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/artist/artistMin.html',
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

angular.module('claudeApp').
    directive('artistFacebookMin', ['$window', '$rootScope',
        function ($window, $rootScope) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/artist/artistFacebookMin.html',
            controller: 'ArtistFacebookMinCtrl',
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