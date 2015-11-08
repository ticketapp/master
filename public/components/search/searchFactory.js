angular.module('claudeApp').factory('SearchFactory', ['$rootScope', '$location', '$q', 'EventsFactory', 'UserFactory',
    'GenresFactory', 'GeolocFactory', 'CityFactory', '$localStorage',
    function ($rootScope, $location, $q, EventsFactory, UserFactory, GenresFactory, GeolocFactory, CityFactory, $localStorage) {
    var factory = {
        initSearch : false,
        research : '',
        events: [],
        eventsLocked: false,
        isConnected: false,
        eventsOffset: 0,
        init : function () {
            if (factory.initSearch == false) {
                $rootScope.activArtist = false;
                $rootScope.activEvent = true;
                $rootScope.activPlace = false;
                $rootScope.activUsr = false;
                $rootScope.maxStart = 30;
                $rootScope.maxStartView = 168;
                $rootScope.redirectToSearch = function (research) {
                    factory.research = research;
                    $rootScope.storeSearch = research;
                    if ($location.path() != '/') {
                        $location.path('/search')
                    }
                };
                $rootScope.remStoreSearch = function () {
                    $rootScope.storeSearch = '';
                    factory.research = '';
                };
                factory.initSearch = true;
                UserFactory.getIsConnected().then(function (isConnected) {
                    factory.isConnected = isConnected;
                })
            }
        },
        storeSearch : function (search) {
            $rootScope.storeSearch = search;
            factory.research = search;
        },
        filterEventArtists: function (event, research) {
            return event.artists.filter(function (artist) {
                    return artist.name.indexOf(research) > -1
                }).length > 0;
        },
        filterEventPlaces: function (event, research) {
            return event.places.filter(function (place) {
                    return place.name.indexOf(research) > -1
                }).length > 0;
        },
        filterEventOrganizers: function (event, research) {
            return event.organizers.filter(function (organizer) {
                    return organizer.name.indexOf(research) > -1
                }).length > 0;
        }, 
        filterEventAddresses: function (event, research) {
            return event.addresses.filter(function (address) {
                    return address.city.indexOf(research) > -1
                }).length > 0;
        }, 
        filterEvent: function(research) {
            factory.events = factory.events.filter(function(event) {
                return event.name.indexOf(research) > -1 || factory.filterEventPlaces(event, research) ||
                    factory.filterEventAddresses(event, research) || factory.filterEventArtists(event, research) ||
                    factory.filterEventOrganizers(event, research)
            })
        },
        updateEvents: function(events) {
            factory.eventsLocked = true;
            var eventsId = factory.events.map(function(event) {
                return event.id;
            });
            var eventsToPush = events.filter(function (event) {
                return eventsId.indexOf(event.id) === -1
            });
            factory.events = factory.events.concat(eventsToPush);
            factory.eventsLocked = false;
        },
        filterEventsByTime: function(events, start) {
            var eventsLength = events.length;
            var maxStartTime = start * 3600000 + new Date().getTime();
            for (var e = 0; e < eventsLength; e++) {
                if (events[e].startTime > maxStartTime) {
                    events.splice(e, 1);
                    e = e - 1;
                    eventsLength = eventsLength - 1;
                }
            }
        },
        searchEventsWithQuery: function(research, offset) {
            factory.filterEvent(research);
            var deferred = $q.defer();
            if (research === factory.research && factory.events.length > 0 && offset === factory.eventsOffset) {
                deferred.resolve(factory.events)
            } else {
                factory.research = research;
                deferred.notify(factory.events);
                GeolocFactory.getGeolocation().then(function (geolocation) {
                    GenresFactory.isAGenre(research).then(function(isAGenre) {
                        if (isAGenre === true) {
                            EventsFactory.getEventsByGenre(research, offset, geolocation).then(function (events) {
                                if (factory.eventsLocked === false) {
                                    factory.updateEvents(events);
                                    deferred.notify(factory.events)
                                } else {
                                    var waitForEvents = setInterval(function() {
                                        if (factory.eventsLocked === false) {
                                            clearInterval(waitForEvents);
                                            factory.updateEvents(events);
                                            deferred.notify(factory.events)
                                        }
                                    }, 100)
                                }
                            })
                        }
                    });
                    CityFactory.isACity(research).then(function(isACity) {
                        if (isACity === true) {
                            EventsFactory.getEventsByCity(research, offset).then(function (events) {
                                if (factory.eventsLocked === false) {
                                    factory.updateEvents(events);
                                    deferred.notify(factory.events)
                                } else {
                                    var waitForEvents = setInterval(function() {
                                        if (factory.eventsLocked === false) {
                                            clearInterval(waitForEvents);
                                            factory.updateEvents(events);
                                            deferred.notify(factory.events)
                                        }
                                    }, 100)
                                }
                            })
                        }
                    });
                    EventsFactory.getEventsByContaining(research, geolocation).then(function(events) {
                        if (factory.eventsLocked === false) {
                            factory.updateEvents(events);
                            deferred.notify(factory.events)
                        } else {
                            var waitForEvents = setInterval(function() {
                                if (factory.eventsLocked === false) {
                                    clearInterval(waitForEvents);
                                    factory.updateEvents(events);
                                    deferred.notify(factory.events)
                                }
                            }, 100)
                        }
                    });
                    EventsFactory.getArtistsEventsByContaining(research).then(function(events) {
                        if (factory.eventsLocked === false) {
                            factory.updateEvents(events);
                            deferred.notify(factory.events)
                        } else {
                            var waitForEvents = setInterval(function() {
                                if (factory.eventsLocked === false) {
                                    clearInterval(waitForEvents);
                                    factory.updateEvents(events);
                                    deferred.notify(factory.events)
                                }
                            }, 100)
                        }
                    });
                    EventsFactory.getPlacesEventsByContaining(research).then(function(events) {
                        if (factory.eventsLocked === false) {
                            factory.updateEvents(events);
                            deferred.notify(factory.events)
                        } else {
                            var waitForEvents = setInterval(function() {
                                if (factory.eventsLocked === false) {
                                    clearInterval(waitForEvents);
                                    factory.updateEvents(events);
                                    deferred.notify(factory.events)
                                }
                            }, 100)
                        }
                    });
                    EventsFactory.getOrganizersEventsByContaining(research).then(function(events) {
                        if (factory.eventsLocked === false) {
                            factory.updateEvents(events);
                            deferred.notify(factory.events)
                        } else {
                            var waitForEvents = setInterval(function() {
                                if (factory.eventsLocked === false) {
                                    clearInterval(waitForEvents);
                                    factory.updateEvents(events);
                                    deferred.notify(factory.events)
                                }
                            }, 100)
                        }
                    });
                });
            }
            return deferred.promise;
        },
        getEvents: function(start, offset) {
            var deferred = $q.defer();
            factory.filterEventsByTime(factory.events);
            EventsFactory.getEvents(start, $rootScope.geoLoc, offset).then(function (events) {
                if (factory.eventsLocked === false) {
                    factory.updateEvents(events);
                    deferred.resolve(factory.events);
                } else {
                    var waitForEvents = setInterval(function() {
                        if (factory.eventsLocked === false) {
                            clearInterval(waitForEvents);
                            factory.updateEvents(events);
                            deferred.resolve(factory.events)
                        }
                    }, 100)
                }
            });
            return deferred.promise;
        }
    };
    factory.init();
    return factory;
}]);