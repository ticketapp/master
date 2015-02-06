app.controller ('EventViewCtrl', function ($scope, $routeParams, $http ){
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            $scope.event = data;
            console.log(data.name);
        }).error(function(data, status){
            console.log(data);
        });
});
