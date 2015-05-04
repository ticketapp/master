angular.module('claudeApp').factory ('EventsFactory', function ($http, $q){
    var factory = {
        events : false,
        refactorTicketsInfo : function (event) {
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
        getEvents : function (start, geoloc, offset) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/events/inInterval/' + start + '/' + geoloc + '/12/' + offset)
                    .success(function(data, status){
                        data.forEach(factory.colorEvent);
                        factory.events = data;
                        deferred.resolve(factory.events);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        getEvent : function (id) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/events/' + id)
                    .success(function(data, status){
                        data = factory.refactorTicketsInfo(data);
                        factory.events = data;
                        deferred.resolve(factory.events);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        getEventsByContaining : function (pattern, geoloc) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/events/containing/' + pattern + '/' + geoloc)
                    .success(function(data, status){
                        factory.events = data;
                        deferred.resolve(factory.events);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        getArtistsEventsByContaining : function (pattern) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                var artistsEvents = [];
                $http.get('/artists/containing/'+ pattern).
                    success(function(data, status, headers, config) {
                        function getArtistEvents (art) {
                            $http.get('/artists/'+ art.facebookUrl + '/events ').
                                success(function(data){
                                    function pushEvents (event) {
                                        artistsEvents.push(event)
                                    }
                                    data.forEach(pushEvents)
                                    factory.events = artistsEvents;
                                    deferred.resolve(factory.events);
                                })
                        }
                        data.forEach(getArtistEvents);
                    });
            }
            return deferred.promise;
        },
        getPlacesEventsByContaining : function (pattern) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                var placesEvents = [];
                $http.get('/places/containing/'+ pattern).
                    success(function(data, status, headers, config) {
                        function getPlaceEvents (place) {
                            $http.get('/places/'+ place.placeId + '/events ').
                                success(function(data){
                                    function pushEvents (event) {
                                        placesEvents.push(event)
                                    }
                                    data.forEach(pushEvents)
                                    factory.events = placesEvents;
                                    deferred.resolve(factory.events);
                                })
                        }
                        data.forEach(getPlaceEvents)
                    });
            }
            return deferred.promise;
        },
        getEventsByGenre : function (pattern, offset, geoloc) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/genres/'+ pattern +'/12/' + offset + '/events/' + geoloc).
                    success(function(data, status, headers, config) {
                        factory.events = data;
                        deferred.resolve(factory.events);
                    });
            }
            return deferred.promise;
        },
        getEventsByCity : function (pattern, offset) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/events/nearCity/' + pattern + '/12/' + offset ).
                    success(function (data) {
                        factory.events = data;
                        deferred.resolve(factory.events);
                    });
            }
            return deferred.promise;
        },
        postEvent : function (event) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.post('/events/create' + event).
                    success(function (data) {
                        factory.events = data;
                        deferred.resolve(factory.events);
                    }).error(function (data) {
                        factory.events = data;
                        deferred.resolve(factory.events);
                    });
            }
            return deferred.promise;
        }
    };
    return factory;
});

