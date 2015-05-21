angular.module('claudeApp').factory ('EventsFactory', ['$http', '$q', 'StoreRequest', 'InfoModal',
    'ImagesFactory',
    function ($http, $q, StoreRequest, InfoModal, ImagesFactory){
    var factory = {
        events : false,
        refactorTicketsInfo : function (event) {
            event.artists.forEach(ImagesFactory);
            if (event.ticketSellers != undefined) {
                if (event.ticketSellers.indexOf("digitick") > -1) {
                    event.ticketPlatform = "digitick";
                }
                if (event.ticketSellers.indexOf("weezevent") > -1) {
                    event.ticketPlatform = "weezevent";
                }
                if (event.ticketSellers.indexOf("yurplan") > -1) {
                    event.ticketPlatform = "yurplan";
                }
                if (event.ticketSellers.indexOf("eventbrite") > -1) {
                    event.ticketPlatform = "eventbrite";
                }
                if (event.ticketSellers.indexOf("ticketmaster") > -1) {
                    event.ticketPlatform = "ticketmaster";
                }
                if (event.ticketSellers.indexOf("ticketnet") > -1) {
                    event.ticketPlatform = "ticketnet";
                }
            }
            if (event.tariffRange != undefined) {
                var tariffs = event.tariffRange.split('-');

                if (tariffs[1] > tariffs[0]) {
                    event.tariffRange = tariffs[0] + '€ - ' + tariffs[1] + '€';
                } else {
                    event.tariffRange = tariffs[0] + '€';
                }
            }
            return event;
        },
        colorEvent : function (event) {
            event.artists.forEach(ImagesFactory);
            event.priceColor = '#2DAAE1';
            if (event.tariffRange != undefined) {
                var tariffs = event.tariffRange.split('-');
                if (tariffs[1] > tariffs[0]) {
                    event.tariffRange = tariffs[0].replace('.0', '') + ' - ' +
                        tariffs[1].replace('.0', '') + '€';
                } else {
                    event.tariffRange = tariffs[0].replace('.0', '') + '€';
                }
                event.priceColor = 'rgb(' + tariffs[0]*2 + ',' + (200 - (tariffs[0]*4 ) )+
                    ',' + tariffs[0]*4 + ')'
            }
        },
        lastGetEvents : {
            start : 0,
            geoloc : 0,
            offset : -1,
            events : []
        },
        getEvents : function (start, geoloc, offset) {
            var deferred = $q.defer();
            if (start == factory.lastGetEvents.start && geoloc == factory.lastGetEvents.geoloc &&
                offset <= factory.lastGetEvents.offset) {
                console.log(factory.lastGetEvents.events);
                deferred.resolve(factory.lastGetEvents.events);
            } else {
                $http.get('/events/inInterval/' + start + '?geographicPoint=' + geoloc + '&offset=' + offset + '&numberToReturn=12')
                    .success(function (data, status) {
                        data.forEach(factory.colorEvent);
                        if (offset > factory.lastGetEvents.offset) {
                            factory.lastGetEvents.events =  factory.lastGetEvents.events.concat(data);
                        } else {
                            factory.lastGetEvents.events = data;
                        }
                        factory.lastGetEvents.start = start;
                        factory.lastGetEvents.geoloc = geoloc;
                        factory.lastGetEvents.offset = offset;
                        deferred.resolve(factory.lastGetEvents.events);
                    }).error(function (data, status) {
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        lastGetEvent : {id: 0, event: {}},
        getEvent : function (id) {
            var deferred = $q.defer();
            console.log(factory.lastGetEvent)
            if (id == factory.lastGetEvent.id) {
                deferred.resolve(factory.lastGetEvent.event)
            } else {
                $http.get('/events/' + id)
                    .success(function (data, status) {
                        factory.lastGetEvent.id = id;
                        data = factory.refactorTicketsInfo(data);
                        factory.lastGetEvent.event = data;
                        deferred.resolve(factory.lastGetEvent.event);
                    }).error(function (data, status) {
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        lastGetEventsByContaining: {pattern: '', geoloc: 0, events: []},
        getEventsByContaining : function (pattern, geoloc) {
            var deferred = $q.defer();
            if (pattern == factory.lastGetEventsByContaining.pattern &&
                geoloc == factory.lastGetEventsByContaining.geoloc) {
                deferred.resolve(factory.lastGetEventsByContaining.events)
            } else {
                $http.get('/events/containing/' + pattern + '?geographicPoint=' + geoloc)
                    .success(function (data, status) {
                        factory.lastGetEventsByContaining.pattern = pattern;
                        factory.lastGetEventsByContaining.geoloc = geoloc;
                        data.forEach(factory.colorEvent);
                        factory.lastGetEventsByContaining.events = data;
                        deferred.resolve(factory.lastGetEventsByContaining.events);
                    }).error(function (data, status) {
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        lastGetArtistsEventsByContaining: {pattern: '', events: []},
        getArtistsEventsByContaining : function (pattern) {
            var deferred = $q.defer();
            if (pattern == factory.lastGetArtistsEventsByContaining.pattern) {
                deferred.resolve(factory.lastGetArtistsEventsByContaining.events)
            } else {
                var artistsEvents = [];
                $http.get('/artists/containing/' + pattern).
                    success(function (data, status, headers, config) {
                        factory.lastGetArtistsEventsByContaining.pattern = pattern;
                        function getArtistEvents(art) {
                            $http.get('/artists/' + art.facebookUrl + '/events ').
                                success(function (data) {
                                    function pushEvents(event) {
                                        artistsEvents.push(event)
                                    }

                                    data.forEach(factory.colorEvent);
                                    data.forEach(pushEvents);
                                    factory.lastGetArtistsEventsByContaining.events = artistsEvents;
                                    deferred.resolve(factory.lastGetArtistsEventsByContaining.events);
                                })
                        }

                        data.forEach(getArtistEvents);
                    });
            }
            return deferred.promise;
        },
        lastGetPlacesEventsByContaining: {pattern: '', events: []},
        getPlacesEventsByContaining : function (pattern) {
            var deferred = $q.defer();
            if (pattern == factory.lastGetPlacesEventsByContaining.pattern) {
                deferred.resolve(factory.lastGetPlacesEventsByContaining.events)
            } else {
                var placesEvents = [];
                $http.get('/places/containing/' + pattern).
                    success(function (data, status, headers, config) {
                        factory.lastGetPlacesEventsByContaining.pattern = pattern;
                        function getPlaceEvents(place) {
                            $http.get('/places/' + place.placeId + '/events ').
                                success(function (data) {
                                    function pushEvents(event) {
                                        placesEvents.push(event)
                                    }

                                    data.forEach(factory.colorEvent);
                                    data.forEach(pushEvents);
                                    factory.lastGetPlacesEventsByContaining.events = placesEvents;
                                    deferred.resolve(factory.lastGetPlacesEventsByContaining.events);
                                })
                        }

                        data.forEach(getPlaceEvents)
                    });
            }
            return deferred.promise;
        },
        lastGetEventsByGenre: {pattern: '', offset: -1, geoloc: '', events: []},
        getEventsByGenre : function (pattern, offset, geoloc) {
            var deferred = $q.defer();
            if (pattern == factory.lastGetEventsByGenre.pattern &&
                offset <= factory.lastGetEventsByGenre.offset &&
                geoloc == factory.lastGetEventsByGenre.geoloc) {
                deferred.resolve(factory.lastGetEventsByGenre.events)
            } else {
                $http.get('/genres/' + pattern + '/events?geographicPoint=' + geoloc + '&offset=' + offset + '&numberToReturn=12').
                    success(function (data, status, headers, config) {
                        data.forEach(factory.colorEvent);
                        if (offset > factory.lastGetEventsByGenre.offset) {
                            factory.lastGetEventsByGenre.events =
                                factory.lastGetEventsByGenre.events.concat(data);
                        } else {
                            factory.lastGetEventsByGenre.events = data;
                        }
                        factory.lastGetEventsByGenre.pattern = pattern;
                        factory.lastGetEventsByGenre.offset = offset;
                        factory.lastGetEventsByGenre.geoloc = geoloc;
                        deferred.resolve(factory.lastGetEventsByGenre.events);
                    });
            }
            return deferred.promise;
        },
        lastGetEventsByCity: {pattern: '', offset: -1, events: []},
        getEventsByCity : function (pattern, offset) {
            var deferred = $q.defer();
            if (pattern == factory.lastGetEventsByCity.pattern &&
                offset <= factory.lastGetEventsByCity.offset) {
                deferred.resolve(factory.lastGetEventsByCity.events)
            } else {
                $http.get('/events/nearCity/' + pattern + '?numberToReturn=12&offset=' + offset).
                    success(function (data) {
                        data.forEach(factory.colorEvent);
                        if (offset > factory.lastGetEventsByCity.offset) {
                            factory.lastGetEventsByCity.events = factory.lastGetEventsByCity.events.concat(data);
                        } else {
                            factory.lastGetEventsByCity.events = data;
                        }
                        factory.lastGetEventsByCity.pattern = pattern;
                        factory.lastGetEventsByCity.offset = offset;
                        deferred.resolve(factory.lastGetEventsByCity.events);
                    });
            }
            return deferred.promise;
        },
        postEvent : function (event) {
            var deferred = $q.defer();
            $http.post('/events/create' + event).
                success(function (data) {
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function (data) {
                    factory.events = data;
                    deferred.resolve(factory.events);
                });
            return deferred.promise;
        },
        followEventByEventId : function (id, eventName) {
            var deferred = $q.defer();
            $http.post('/events/' + id +'/follow').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/events/' + id +'/follow', "", 'vous suivez ' + eventName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        unfollowEvent : function (id, eventName) {
            var deferred = $q.defer();
            $http.post('/events/' + id +'/unfollow').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/events/' + id +'/unfollow', "", 'vous ne suivez plus ' + eventName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/events/' + id + '/isEventFollowed')
                .success(function(data, status){
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        postEventToCreate : function (facebookId) {
            var deferred = $q.defer();
            $http.post('/events/create/'  + facebookId).
                success(function (data) {
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function (data) {
                    factory.events = data;
                    deferred.resolve(factory.events);
                });
            return deferred.promise;
        }
    };
    return factory;
}]);

