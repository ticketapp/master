angular.module('claudeApp').factory('DetectSize', ['mediaQueries', '$window', '$rootScope',
    function (mediaQueries, $window, $rootScope) {
    var size = false;
    function watchSize () {
        function detectSize() {
            if (mediaQueries.large() == true) {
                size = 'large'
            } else if (mediaQueries.medium() == true) {
                size = 'medium'
            } else if (mediaQueries.small() == true) {
                size = 'small'
            }
            $rootScope.window = size;
            return size;
        }
        detectSize();
    }
    watchSize();
    $window.addEventListener('resize', watchSize);
    return size;
}]);