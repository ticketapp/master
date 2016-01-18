angular.module('claudeApp').factory ('OrganizerFactory',['$http', '$q', 'EventsFactory', 'StoreRequest',
    'InfoModal', 'RoutesFactory', 'RefactorObjectsFactory', 'GeolocFactory',
    function ($http, $q, EventsFactory, StoreRequest, InfoModal, RoutesFactory, RefactorObjectsFactory, GeolocFactory) {
    var factory = {
        organizers: false,
        lastOrganizer: {id: '', organizer: {}},

        normalizedOrganizerObject : function (organizer) {
          return organizer.organizer
        },

        getOrganizer : function(id) {
            var deferred = $q.defer();
            if (id == factory.lastOrganizer.id) {
                deferred.resolve(factory.lastOrganizer.organizer)
            } else {
                $http.get('/organizers/' + id).
                    success(function(data) {
                        factory.lastOrganizer.id = id;
                        factory.lastOrganizer.organizer = factory.normalizedOrganizerObject(data);
                        deferred.resolve(factory.lastOrganizer.organizer);
                    })
            }
            return deferred.promise;
        },

        lastOrganizerEvents : {id: '', events: []},
        getOrganizerEvents : function(id) {
            var deferred = $q.defer();
            if(id == factory.lastOrganizerEvents.id) {
                deferred.resolve(factory.lastOrganizerEvents.events);
            } else {
                $http.get('/organizers/' + id + '/events').
                    success(function(events) {
                        events = events.map(function(event) {
                            return RefactorObjectsFactory.normalizeEventObject(event)
                        });
                        factory.lastOrganizerEvents.id = id;
                        events.forEach(EventsFactory.colorEvent);
                        factory.lastOrganizerEvents.events = events;
                        deferred.resolve(factory.lastOrganizerEvents.events);
                    })
            }
            return deferred.promise;
        },

        lastGetOrganizers : {offset: -1, organizers: []},
        getOrganizers : function(offset) {
            var deferred = $q.defer();
            if(factory.lastGetOrganizers.offset >= offset) {
                deferred.resolve(factory.lastGetOrganizers.organizers);
            } else {
                GeolocFactory.getGeolocation().then(function(geographicPoint) {
                    $http.get('/organizers/findNearGeoPoint/?geographicPoint=' + geographicPoint +
                        '&numberToReturn=12&offset=' + offset).
                    success(function(data) {
                        var organizers = data.map(function (organizer) {
                            return factory.normalizedOrganizerObject(organizer)
                        });
                        factory.lastGetOrganizers.offset = offset;
                        factory.lastGetOrganizers.organizers =
                            factory.lastGetOrganizers.organizers.concat(organizers);
                        deferred.resolve(factory.lastGetOrganizers.organizers);
                    })
                });
            }
            return deferred.promise;
        },

        lastGetOrganizersByContaining: {pattern: '', organizers: []},
        getOrganizersByContaining : function(pattern) {
            var deferred = $q.defer();
            if(factory.lastGetOrganizersByContaining.pattern === pattern) {
                deferred.resolve(factory.lastGetOrganizersByContaining.organizers);
            } else {
                $http.get('/organizers/containing/'+ pattern).
                    success(function(data) {
                        var organizers = data.map(function (organizer) {
                            return factory.normalizedOrganizerObject(organizer);
                        });
                        factory.lastGetOrganizersByContaining.organizers = organizers;
                        factory.lastGetOrganizersByContaining.pattern = pattern;
                        deferred.resolve(factory.lastGetOrganizersByContaining.organizers);
                    })
            }
            return deferred.promise;
        },

        followOrganizerByFacebookId : function(id) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id + '/followByFacebookId ').
                success(function(data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/artists/' + id +'/followByFacebookId', "", '')
                    }
                    deferred.reject(data);
                });
            return deferred.promise;
        },
        followOrganizerByOrganizerId : function(id, organizerName) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id +'/followByOrganizerId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/organizers/' + id +'/followByOrganizerId',
                            "", 'vous suivez ' + organizerName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        unfollowOrganizer : function (id, organizerName) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id +'/unfollowOrganizerByOrganizerId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/organizers/' + id +'/unfollowOrganizerByOrganizerId',
                            "", 'vous ne suivez plus ' + organizerName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/organizers/' + id + '/isFollowed')
                .success(function(data) {
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function() {
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        createOrganizer : function(organizer) {
            var deferred = $q.defer();
            $http.post('/organizers/create', organizer).
                success(function(data) {
                    deferred.resolve(data);
                }).error(function () {
                    deferred.resolve('error');
                });
            return deferred.promise;
        },
        lastGetPlaceEvents: {id: '', events: []},
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
        lastGetPassedEvents: {id: '', events: []},
        getPassedEvents : function(id) {
            var deferred = $q.defer();
            if(factory.lastGetPassedEvents.id == id) {
                deferred.resolve(factory.lastGetPassedEvents.events);
            } else {
                $http.get(RoutesFactory.organizers.getOrganizersPassedEvents(id)).
                    success(function(events) {
                        events = events.map(function(event) {
                            return RefactorObjectsFactory.normalizeEventObject(event)
                        });
                        events.forEach(EventsFactory.colorEvent);
                        factory.lastGetPassedEvents.events = events;
                        factory.lastGetPassedEvents.id = id;
                        deferred.resolve(factory.lastGetPassedEvents.events);
                    })
            }
            return deferred.promise;
        },
        getFollowedOrganizers : function () {
            var defered = $q.defer();
            $http.get('/organizers/followed/ ').success(function (data) {
                defered.resolve(data);
            });
            return defered.promise;
        }
    };
    return factory;
}]);
