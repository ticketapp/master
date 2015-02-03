app.controller ('EventViewCtrl', function ($scope, EventFactory, $routeParams, $http ){
    $http.get('/event/' + $routeParams.id)
        .success(function(data, status){
            $scope.event = data;
        }).error(function(data, status){
            console.log(data);
        });
});
