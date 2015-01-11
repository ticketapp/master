
	app.controller ('ArtistesCtrl', function ($scope, ArtisteFactory, $routeParams ){
	var artiste = ArtisteFactory.getArtiste($routeParams.id).then(function(artiste){
	$scope.artiste = artiste;
	}, function(msg){
		alert (msg);
		})
	});
