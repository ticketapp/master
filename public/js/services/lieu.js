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
            },
            getPlacesByContaining : function (pattern) {
                var deferred = $q.defer();
                if(factory.places == true) {
                    deferred.resolve(factory.places);
                } else {
                    $http.get('/places/containing/'+ pattern)
                        .success(function(data, status) {
                            factory.places = data;
                            deferred.resolve(factory.places);
                    }).error(function(data, status) {
                        deferred.reject('erreur');
                    });
                }
                return deferred.promise;
            },
            getPlacesByCity : function (pattern, offset) {
                var deferred = $q.defer();
                if(factory.places == true) {
                    deferred.resolve(factory.places);
                } else {
                    $http.get('/places/nearCity/' +  pattern + '/12/' + offset)
                        .success(function(data, status) {
                            factory.places = data;
                            deferred.resolve(factory.places);
                    }).error(function(data, status) {
                        deferred.reject('erreur');
                    });
                }
                return deferred.promise;
            },
            followPlaceByFacebookId : function (id) {
                var deferred = $q.defer();
                if(factory.places == true) {
                    deferred.resolve(factory.places);
                } else {
                    $http.post('/places/' +  id + '/followByFacebookId')
                        .success(function(data, status) {
                            factory.places = data;
                            deferred.resolve(factory.places);
                    }).error(function(data, status) {
                        deferred.resolve('error');
                    });
                }
                return deferred.promise;
            },
            postPlace : function (place) {
                var deferred = $q.defer();
                if(factory.places == true) {
                    deferred.resolve(factory.places);
                } else {
                    console.log(place)
                    $http.post('/places/create', place )
                        .success(function(data, status) {
                            factory.places = data;
                            deferred.resolve(factory.places);
                    }).error(function(data, status) {
                        deferred.resolve('error');
                    });
                }
                return deferred.promise;
            }
		};
		return factory;
});
	