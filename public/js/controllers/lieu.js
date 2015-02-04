	
	app.controller ('PlaceCtrl', function ($scope, $http, $routeParams ){
        $http.get('/places/'+ $routeParams.id).
            success(function (data, status, headers, config){
                $scope.place = data;
            });
        $http.get('/places/'+ $routeParams.id + '/events').
            success(function (data, status, headers, config){
                $scope.events = data;
            })
	});
