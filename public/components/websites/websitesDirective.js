angular.module('claudeApp').
    directive('websites', function () {
        return {
            restrict : 'E',
            templateUrl: 'assets/components/websites/websites.html'
        }
    });