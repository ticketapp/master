
angular.module('claudeApp').directive('ngIssues', function(){
    return{
        restrict : 'AE',
        controller: 'issuesCtrl',
        templateUrl:'components/issues/issues.html'
    }
});