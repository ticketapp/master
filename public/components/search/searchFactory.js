angular.module('claudeApp').factory('SearchFactory', ['$rootScope', '$location', '$q', 'EventsFactory', 'UserFactory',
    'GenresFactory', 'GeolocFactory', 'CityFactory',
    function ($rootScope, $location, $q, EventsFactory, UserFactory, GenresFactory, GeolocFactory, CityFactory) {
    var factory = {
        initSearch : false,
        research : false,
        events: [],
        eventsLocked: false,
        isConnected: false,
        eventsOffset: 0,
        init : function () {
            if (factory.initSearch === false) {
                $rootScope.activArtist = false;
                $rootScope.activEvent = true;
                $rootScope.activPlace = false;
                $rootScope.activUsr = false;
                $rootScope.maxStart = 30;
                $rootScope.maxStartView = 168;
                $rootScope.redirectToSearch = function (research) {
                    factory.research = research;
                    $rootScope.storeSearch = research;
                    if ($location.path() !== '/') {
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
        searchEventsWithQuery: function(research, offset) {
            factory.filterEvent(research);
            var deferred = $q.defer();

            function waitForUpdateEvents(events) {
                if (factory.eventsLocked === false) {
                    factory.updateEvents(events);
                    deferred.notify(factory.events)
                } else {
                    var waitForEvents = setInterval(function () {
                        if (factory.eventsLocked === false) {
                            clearInterval(waitForEvents);
                            factory.updateEvents(events);
                            deferred.notify(factory.events)
                        }
                    }, 100)
                }
            }

            if (research === factory.research &&
                factory.events.length > 0 && offset === factory.eventsOffset) {
                deferred.resolve(factory.events)
            } else {
                factory.research = research;
                factory.eventsOffset = offset;
                deferred.notify(factory.events);
                GeolocFactory.getGeolocation().then(function (geolocation) {
                    GenresFactory.isAGenre(research).then(function(isAGenre) {
                        if (isAGenre === true) {
                            EventsFactory.getEventsByGenre(research, offset, geolocation).then(function (events) {
                                waitForUpdateEvents(events);
                            })
                        }
                    });
                    CityFactory.isACity(research).then(function(isACity) {
                        if (isACity) {
                            EventsFactory.getEventsByCity(research, offset).then(function (events) {
                                waitForUpdateEvents(events);
                            })
                        }
                    });
                    EventsFactory.getEventsByContaining(research, geolocation).then(function(events) {
                        waitForUpdateEvents(events);
                    });
                    EventsFactory.getArtistsEventsByContaining(research).then(function(events) {
                        waitForUpdateEvents(events);
                    });
                    EventsFactory.getPlacesEventsByContaining(research).then(function(events) {
                        waitForUpdateEvents(events);
                    });
                    EventsFactory.getOrganizersEventsByContaining(research).then(function(events) {
                        waitForUpdateEvents(events);
                    });
                });
            }
            return deferred.promise;
        },
        filterByTime: function (start, event) {
            var now = new Date().getTime();
            var twelveHoursAgo = new Date().getTime() - 3600000*12;
            var xHoursLater = start * 3600000 + new Date().getTime();
            return ((event.endTime !== undefined && event.endTime < xHoursLater && event.endTime > now) ||
                (event.endTime === undefined && event.startTime < xHoursLater && event.startTime >= twelveHoursAgo ))
        },
        getEvents: function(start, offset, geolocation) {
            var deferred = $q.defer();
            factory.events = factory.events.filter(function(event) {
                return factory.filterByTime(start, event);
            });
            factory.research = false;
            deferred.notify(factory.events);
            if (!geolocation) {
                geolocation = $rootScope.geoLoc;
            }
            EventsFactory.getEvents(start, geolocation, offset).then(function (events) {
                if (factory.eventsLocked === false) {
                    events = events.filter(function(event) {
                        return factory.filterByTime(start, event);
                    });
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