angular.module('claudeApp').factory ('PlaceFactory', ['$http', '$q', 'EventsFactory',
    function ($http, $q, EventsFactory){
    var factory = {
        places : false,
        getPlace : function(id) {
            var deferred = $q.defer();
            if(factory.organizers == true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.get('/places/' + id).
                    success(function(data, status, headers, config) {
                        factory.organizers = data;
                        deferred.resolve(factory.organizers);
                    })
            }
            return deferred.promise;
        },
        getPlaceEvents : function(id) {
            var deferred = $q.defer();
            if(factory.organizers == true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.get('/places/' + id + '/events').
                    success(function(data, status, headers, config) {
                        data.forEach(EventsFactory.colorEvent);
                        factory.organizers = data;
                        deferred.resolve(factory.organizers);
                    })
            }
            return deferred.promise;
        },
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
                        console.log(data)
                        deferred.resolve('error');
                    });
            }
            return deferred.promise;
        }
    };
    return factory;
}]);
