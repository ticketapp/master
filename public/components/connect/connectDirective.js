
angular.module('claudeApp').directive('ngConnect', function(){
    return{
        restrict : 'E',
        controller : 'connectCtrl',
        templateUrl : 'assets/components/connect/connectForm.html'
    }
});