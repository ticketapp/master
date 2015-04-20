app.factory ('EventsFactory', function ($http, $q){
    var factory = {
        events : false,
        getEvents : function (start, geoloc, offset) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/events/inInterval/' + start + '/' + geoloc + '/12/' + offset)
                    .success(function(data, status){
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
        getEventsByGenre : function (pattern, offset) {
            var deferred = $q.defer();
            if(factory.events == true){
                deferred.resolve(factory.events);
            } else {
                $http.get('/genres/'+ pattern +'/12/' + offset + '/events ').
                    success(function(data, status, headers, config) {
                        factory.events = data;
                        deferred.resolve(factory.events);
                    });
            }
            return deferred.promise;
        }
    };
    return factory;
});

