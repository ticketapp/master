app.factory ('PlaceFactory', function ($http, $q){
		var factory = {
			places : false,
            getPlaces : function (offset, geoLoc) {
                var deferred = $q.defer();
                if(factory.places == true) {
                    deferred.resolve(factory.places);
                } else {
                    $http.get('/places/' + geoLoc + '/12/' + offset)
                        .success(function(data, status) {
                            factory.places = data;
                            deferred.resolve(factory.places);
                    }).error(function(data, status) {
                        deferred.reject('erreur');
                    });
                }
                return deferred.promise;
            }
		};
		return factory;
});
	