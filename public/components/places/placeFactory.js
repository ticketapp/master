angular.module('claudeApp').factory ('PlaceFactory', ['$http', '$q', 'EventsFactory', 'StoreRequest',
    'InfoModal',
    function ($http, $q, EventsFactory, StoreRequest, InfoModal){
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
                $http.get('/places?geographicPoint='+geoLoc+'&numberToReturn=12&offset='+offset)
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
                $http.get('/places/nearCity/'+pattern+'?numberToReturn=12&offset='+offset)
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
        followPlaceByPlaceId : function (id, placeName) {
            var deferred = $q.defer();
            $http.post('/places/' + id +'/followByPlaceId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/places/' + id +'/followByPlaceId',
                            "", 'vous suivez ' + placeName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite');
                    }
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        unfollowPlace : function (id, placeName) {
            var deferred = $q.defer();
            $http.post('/places/' + id +'/unfollowPlaceByPlaceId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/places/' + id +'/unfollowPlaceByPlaceId',
                            "", 'vous ne suivez plus ' + placeName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite');
                    }
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/places/' + id + '/isFollowed')
                .success(function(data, status){
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        postPlace : function (place) {
            var deferred = $q.defer();
            if(factory.places == true) {
                deferred.resolve(factory.places);
            } else {
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
}]);
