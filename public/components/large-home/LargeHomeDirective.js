angular.module('claudeApp').directive('largeHome', function () {
    return {
        restrict : 'E',
        templateUrl: 'assets/components/large-home/largeHome.html',
        controller: 'LargeHomeCtrl'
    }
});