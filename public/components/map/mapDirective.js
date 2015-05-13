angular.module('claudeApp')
    .directive('mapControls', function(){
    return{
        restrict : 'E',
        templateUrl:'assets/components/map/mapControls.html'
    }
});

angular.module('claudeApp')
    .directive('mapContainer', function(){
        return{
            restrict : 'C',
            controller:'MapCtrl'
        }
    });

angular.module('claudeApp')
    .directive('stylizedMap', function(){
        return{
            restrict : 'E',
            templateUrl:'assets/components/map/map.html'
        }
    });