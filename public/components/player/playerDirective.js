angular.module('claudeApp').directive('ngPlayer', function(){
    return{
        restrict : 'C',
        controller : 'PlayerCtrl',
        templateUrl : 'assets/components/player/player.html'
    }
});