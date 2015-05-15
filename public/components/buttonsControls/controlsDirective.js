angular.module('claudeApp').directive('ngControls', ['$rootScope', '$window',
    function($rootScope, $window){
    return{
        restrict : 'E',
        templateUrl:'assets/components/buttonsControls/controls.html',
        scope : true,
        link : function (scope, element) {
            if ($rootScope.window != 'small') {
                var visible = true;
                function otherListener() {
                    if (window.pageYOffset < 50 && visible == false) {
                        visible = true;
                        $(element).css('margin-right', '0');
                        if ( document.getElementById('infosTooltip') != undefined) {
                            var infosTooltip = document.getElementById('infosTooltip');
                            infosTooltip.classList.remove('ng-hide');
                            infosTooltip.classList.remove('fadeOut');
                            infosTooltip.classList.add('fadeIn')
                        }
                    } else if (window.pageYOffset > 50 && visible == true) {
                        visible = false;
                        $(element).css('margin-right', '-60px');
                        if ( document.getElementById('infosTooltip') != undefined) {
                            var infosTooltip = document.getElementById('infosTooltip');
                            infosTooltip.classList.remove('fadeIn');
                            infosTooltip.classList.add('fadeOut');
                            $timeout(function () {
                                infosTooltip.classList.add('ng-hide')
                            }, 500)
                        }
                    }
                }
                $window.addEventListener('scroll', otherListener);
                scope.$on('$destroy', function () {
                    $window.removeEventListener('scroll', otherListener);
                })
            }
        }
    }

}]);