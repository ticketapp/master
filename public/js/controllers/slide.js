app.controller('SlideCtrl', function($scope, $route, $window, $timeout, $anchorScroll){
    $scope.ui={
        direction : ''
    };
    $scope.$on("$routeChangeSuccess", function(event, next, current){
        $timeout(function(){
            $scope.ui.direction = 'rightmv';
        }, 1000);
    });
});
