	
	app.controller ('PlaceCtrl', function ($scope, $http, $routeParams ){
        $http.get('/place/'+ $routeParams.id).
            success(function (data, status, headers, config){
                $scope.place = data;
            })
	});
