
	app.controller ('ArtistesCtrl', function ($scope, ArtisteFactory, $routeParams, $http ){
        $http.get('/artists/' + $routeParams.id)
            .success(function(data, status){
                $scope.artiste = data;
                console.log(data);
            }).error(function(data, status){
                console.log(data);
            });
	});
