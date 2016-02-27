angular.module('claudeApp').factory ('PlaceFactory', ['$http', '$q', 'EventsFactory', 'StoreRequest',
    'InfoModal', 'RoutesFactory', 'RefactorObjectsFactory',
    function ($http, $q, EventsFactory, StoreRequest, InfoModal, RoutesFactory, RefactorObjectsFactory){
    var factory = {
        places : false,
        lastGetPlace: {id: 0, place: []},
        getPlace : function(id) {
            var deferred = $q.defer();
            if(factory.lastGetPlace.id === id) {
                deferred.resolve(factory.lastGetPlace.place);
            } else {
                $http.get('/places/' + id).
                    success(function(place) {
                        place = RefactorObjectsFactory.refactorPlaceObject(place);
                        factory.lastGetPlace.place = place;
                        factory.lastGetPlace.id = id;
                        deferred.resolve(factory.lastGetPlace.place);
                    })
            }
            return deferred.promise;
        },
        lastGetPlaceEvents: {id: 0, events: []},
        getPlaceEvents : function(id) {
            var deferred = $q.defer();
            if(factory.lastGetPlaceEvents.id === id) {
                deferred.resolve(factory.lastGetPlaceEvents.events);
            } else {
                $http.get('/places/' + id + '/events').
                    success(function(events) {
                        events = events.map(function(event) {
                            return RefactorObjectsFactory.normalizeEventObject(event)
                        });
                        events.forEach(EventsFactory.colorEvent);
                        factory.lastGetPlaceEvents.events = events;
                        factory.lastGetPlaceEvents.id = id;
                        deferred.resolve(factory.lastGetPlaceEvents.events);
                    })
            }
            return deferred.promise;
        },
        lastGetPlaces: {offset: -1, geoloc: 0, timestamp: 0, places: []},
        getPlaces : function (offset, geoLoc) {
            var deferred = $q.defer();
            if(factory.lastGetPlaces.offset >= offset &&
                factory.lastGetPlaces.geoloc === geoLoc && new Date() < factory.lastGetPlaces.timestamp + 180000) {
                deferred.resolve(factory.lastGetPlaces.places);
            } else {
                $http.get('/places?geographicPoint='+geoLoc+'&numberToReturn=12&offset='+offset)
                    .success(function(data, status) {
                        data = data.map(function(place) {
                            return RefactorObjectsFactory.refactorPlaceObject(place);
                        });
                        if (offset > factory.lastGetPlaces.offset) {
                            factory.lastGetPlaces.places = factory.lastGetPlaces.places.concat(data);
                        } else {
                            factory.lastGetPlaces.places = data;
                        }
                        factory.lastGetPlaces.offset = offset;
                        factory.lastGetPlaces.geoloc = geoLoc;
                        factory.lastGetPlaces.timestamp = new Date();
                        deferred.resolve(factory.lastGetPlaces.places);
                    }).error(function() {
                        deferred.reject('error');
                    });
            }
            return deferred.promise;
        },
        lastGetPlacesByContaining: {pattern: '', places: []},
        getPlacesByContaining : function (pattern) {
            var deferred = $q.defer();
            if(factory.lastGetPlacesByContaining.pattern === pattern) {
                deferred.resolve(factory.lastGetPlacesByContaining.places);
            } else {
                $http.get('/places/containing/'+ pattern)
                    .success(function(places) {
                        places = places.map(function(place) {
                            return RefactorObjectsFactory.refactorPlaceObject(place);
                        });
                        factory.lastGetPlacesByContaining.places = places;
                        factory.lastGetPlacesByContaining.pattern = pattern;
                        deferred.resolve(factory.lastGetPlacesByContaining.places);
                    }).error(function() {
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        lastGetPlacesByCity: {pattern: '', offset: -1, places: []},
        getPlacesByCity : function (pattern, offset) {
            var deferred = $q.defer();
            if(factory.lastGetPlacesByCity.pattern === pattern &&
                factory.lastGetPlacesByCity.offset >= offset) {
                deferred.resolve(factory.lastGetPlacesByCity.places);
            } else {
                $http.get('/places/nearCity/'+pattern+'?numberToReturn=12&offset='+offset)
                    .success(function(places) {
                        places = places.map(function(place) {
                            return RefactorObjectsFactory.refactorPlaceObject(place);
                        });
                        if (offset > factory.lastGetPlacesByCity.offset) {
                            factory.lastGetPlacesByCity.places = factory.lastGetPlacesByCity.places.concat(places);
                        } else {
                            factory.lastGetPlacesByCity.places = places;
                        }
                        factory.lastGetPlacesByCity.pattern = pattern;
                        factory.lastGetPlacesByCity.offset = offset;
                        deferred.resolve(factory.lastGetPlacesByCity.places);
                    }).error(function() {
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        followPlaceByFacebookId : function (id) {
            var deferred = $q.defer();
            $http.post('/places/' +  id + '/followByFacebookId')
                .success(function(data) {
                    factory.places = data;
                    deferred.resolve(factory.places);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/artists/' + id +'/followByFacebookId', "", '')
                    }
                    deferred.reject(status);
                });
            return deferred.promise;
        },
        followPlaceByPlaceId : function (id, placeName) {
            var deferred = $q.defer();
            $http.post('/places/' + id +'/followByPlaceId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/places/' + id +'/followByPlaceId',
                            "", 'vous suivez ' + placeName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
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
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/places/' + id +'/unfollowPlaceByPlaceId',
                            "", 'vous ne suivez plus ' + placeName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/places/' + id + '/isFollowed')
                .success(function(data) {
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function() {
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        postPlace : function (place) {
            var deferred = $q.defer();
            $http.post('/places', place )
                .success(function(data) {
                    factory.places = data;
                    deferred.resolve(factory.places);
                }).error(function() {
                    deferred.resolve('error');
                });
            return deferred.promise;
        },
        lastPassedEvents : {id: '', events: []},
        getPassedEvents : function (placeId) {
            var defered = $q.defer();
            if (placeId === factory.getPassedEvents.id) {
                defered.resolve(factory.getPassedEvents.events)
            } else {
                $http.get(RoutesFactory.places.getPlacesPassedEvents(placeId)).success(
                    function (events) {
                        events = events.map(function(event) {
                            return RefactorObjectsFactory.normalizeEventObject(event)
                        });
                        events.forEach(EventsFactory.colorEvent);
                        factory.getPassedEvents.id = placeId;
                        factory.getPassedEvents.events = events;
                        defered.resolve(factory.getPassedEvents.events)
                    }
                ).error(function (data, status) {
                    })
            }
            return defered.promise;
        },
        getFollowedPlaces : function () {
            var defered = $q.defer();
            $http.get('/places/followed/ ').success(function (data) {
                factory.places = data;
                defered.resolve(factory.places);
            });
            return defered.promise;
        }
    };
    return factory;
}]);
