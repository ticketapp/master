angular.module('claudeApp').factory('ToolsFactory', ['$http', '$q', '$rootScope', 'EventsFactory', '$filter',
    function ($http, $q, $rootScope, EventsFactory, $filter) {
    var factory = {
        eventsPlaylist : {
            geopoint: '',
            playlist: {name: 'Evenements aux alentours', tracks: []}
        },
        getEventsPlaylist : function () {
            var deferred = $q.defer();
            if ($rootScope.geoLoc == factory.eventsPlaylist.geopoint) {
                deferred.resolve(factory.eventsPlaylist.playlist)
            } else {
                EventsFactory.getEvents(336, $rootScope.geoLoc, 0).then(function(events) {
                    events = $filter('orderBy')(events, 'startTime', false);
                    var eventsLength = events.length;
                    var tracks = [];
                    for (var i = 0; i < eventsLength; i ++) {
                        if (events[i].artists.length > 0) {
                            for (var j = 0; j < events[i].artists.length; j++) {
                                for (var k = 0; k < 5; k++) {
                                    if (events[i].artists[j].tracks[k] != undefined) {
                                        tracks.push(events[i].artists[j].tracks[k])
                                    }
                                }
                            }
                        }
                    }
                    if (tracks.length > 0) {
                        factory.eventsPlaylist.playlist.tracks = $filter('orderBy')(tracks, 'confidence', true);
                        factory.eventsPlaylist.geopoint = $rootScope.geoLoc;
                        deferred.resolve(factory.eventsPlaylist.playlist)
                    } else {
                        deferred.reject()
                    }
                })
            }
            return deferred.promise;
        },
        eventsGenrePlaylist : {
            geopoint: '',
            genre : '',
            playlist: {name: 'Playlist des evénéments ', tracks: []}
        },
        getEventsGenrePlaylist : function (genre) {
            var deferred = $q.defer();
            if ($rootScope.geoLoc == factory.eventsGenrePlaylist.geopoint &&
                genre == factory.eventsGenrePlaylist.genre) {
                deferred.resolve(factory.eventsGenrePlaylist.playlist)
            } else {
                EventsFactory.getEventsByGenre(genre, 0, $rootScope.geoLoc).then(function(events) {
                    events = $filter('orderBy')(events, 'startTime', false);
                    var eventsLength = events.length;
                    var tracks = [];
                    for (var i = 0; i < eventsLength; i ++) {
                        if (events[i].artists.length > 0) {
                            for (var j = 0; j < events[i].artists.length; j++) {
                                for (var k = 0; k < 5; k++) {
                                    if (events[i].artists[j].tracks[k] != undefined) {
                                        tracks.push(events[i].artists[j].tracks[k])
                                    }
                                }
                            }
                        }
                    }
                    if (tracks.length > 0) {
                        factory.eventsGenrePlaylist.playlist.tracks = $filter('orderBy')(tracks, 'confidence', true);
                        factory.eventsGenrePlaylist.geopoint = $rootScope.geoLoc;
                        factory.eventsGenrePlaylist.genre = genre;
                        factory.eventsGenrePlaylist.playlist.name = 'Playlist des evénéments ' + genre;
                        deferred.resolve(factory.eventsGenrePlaylist.playlist)
                    } else {
                        deferred.reject()
                    }
                })
            }
            return deferred.promise;
        }
    };
    return factory;
}]);