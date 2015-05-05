angular.module('claudeApp').
    directive('artistMin', ['$window', '$rootScope',
        function ($window, $rootScope) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/artist/artistMin.html',
            scope : true,
            link : function (scope, element) {
                function resizeElem () {
                    var waitForElem = setInterval(function () {
                        if ($(element).innerWidth() > 50) {
                            clearInterval(waitForElem);
                            $(element).find('.img_min').css('height', Math.ceil($(element).innerWidth() * 0.376));
                            if ($rootScope.window != 'small') {
                                $(element).css('height', 94 + Math.ceil(($(element).innerWidth() * 0.376) / 2));
                            }
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
    directive('artistFacebookMin', ['$window', '$rootScope',
        function ($window, $rootScope) {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/artist/artistFacebookMin.html',
            controller: 'ArtistFacebookMinCtrl',
            scope : true,
            link : function (scope, element) {
                function resizeElem () {
                    var waitForElem = setInterval(function () {
                        if ($(element).innerWidth() > 50) {
                            clearInterval(waitForElem);
                            $(element).find('.img_min').css('height', Math.ceil($(element).innerWidth() * 0.376));
                            if ($rootScope.window != 'small') {
                                $(element).css('height', 94 + Math.ceil(($(element).innerWidth() * 0.376) / 2));
                            }
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