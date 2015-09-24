angular.module('claudeApp').directive('ngContact', function(){
    return{
        restrict : 'E',
        templateUrl:'assets/components/contact/_contact.html',
        controller : 'contactCtrl'
    }
});