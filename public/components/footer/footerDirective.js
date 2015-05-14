angular.module('claudeApp').directive('footer', function(){
    return{
        restrict : 'C',
        templateUrl:'assets/components/footer/footer.html',
        controller : 'footerCtrl'
    }
});