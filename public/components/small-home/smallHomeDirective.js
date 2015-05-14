angular.module('claudeApp').directive('smallHome', function () {
    return {
        restrict: 'E',
        controller: 'SmallHomeCtrl',
        templateUrl: 'assets/components/small-home/smallHome.html'
    }
});