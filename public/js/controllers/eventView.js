app.controller ('EventViewCtrl', function ($scope, EventFactory, $routeParams, $http ){
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            $scope.event = data;
            console.log(data);
        }).error(function(data, status){
            console.log(data);
        });
});
