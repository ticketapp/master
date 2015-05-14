
angular.module('claudeApp').directive('ngIssues', function(){
    return{
        restrict : 'AE',
        controller: 'issuesCtrl',
        templateUrl:'assets/components/issues/issues.html'
    }
});