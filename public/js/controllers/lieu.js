	
	app.controller ('LieuCtrl', function ($scope, LieuFactory, $routeParams ){
	var lieu = LieuFactory.getLieu($routeParams.id).then(function(lieu){
    $scope.lieu = lieu
	}, function(msg){
	alert (msg) ;
	})
	});
