angular.module('claudeApp').service('PlaylistService', ['$q', '$rootScope', 'EventsFactory', '$filter', 'TrackService',
    'UUID',
    function ($q, $rootScope, EventsFactory, $filter, TrackService, UUID) {
        var factory = {
            eventsPlaylist : {
                geopoint: '',
                playlist: {uuid: UUID.guid(), name: 'Événements aux alentours', tracks: []}
            },
            getEventsPlaylist : function () {
                var deferred = $q.defer();
                if ($rootScope.geoLoc == factory.eventsPlaylist.geopoint) {
                    deferred.resolve(factory.eventsPlaylist.playlist)
                } else {
                    var timeOut;
                    var time = 2000;
                    EventsFactory.getEvents(336, $rootScope.geoLoc, 0, 50).then(function(events) {
                        var collectedTracks = [];
                        events.map(function(event) {
                            if (event.artists) {
                                event.artists.map(function (artist) {
                                    TrackService.getArtistTracks(artist.facebookUrl).then(function (tracks) {
                                        for (var k = 0; k < 5; k++) {
                                            if (tracks[k] != undefined) {
                                                tracks[k].genres = artist.genres;
                                                tracks[k].nextEventId = event.id;
                                                collectedTracks.push(tracks[k])
                                            }
                                        }
                                        if (collectedTracks.length > 0) {
                                            factory.eventsPlaylist.playlist.tracks = $filter('orderBy')(collectedTracks, 'confidence', true);
                                            factory.eventsPlaylist.geopoint = $rootScope.geoLoc;
                                            deferred.notify(factory.eventsPlaylist.playlist)
                                        }
                                    })
                                })
                            }
                            clearTimeout(timeOut);
                            timeOut = setTimeout(function() {
                                deferred.resolve(factory.eventsPlaylist.playlist)
                            }, time)
                        });
                    })
                }
                return deferred.promise;
            },
            eventsGenrePlaylist : {
                uuid: UUID.guid(),
                geopoint: '',
                genre : '',
                playlist: {name: 'Playlist des événements ', tracks: []}
            },
            getEventsGenrePlaylist : function (genre) {
                var deferred = $q.defer();
                if ($rootScope.geoLoc == factory.eventsGenrePlaylist.geopoint &&
                    genre == factory.eventsGenrePlaylist.genre) {
                    deferred.resolve(angular.copy(factory.eventsGenrePlaylist.playlist))
                } else {
                    var timeOut;
                    var time = 2000;
                    EventsFactory.getEventsByGenre(genre, 0, $rootScope.geoLoc, 50).then(function(events) {
                        var collectedTracks = [];
                        events.map(function(event) {
                            if(event.artists) {
                                event.artists.map(function (artist) {
                                    TrackService.getArtistTracks(artist.facebookUrl).then(function (tracks) {
                                        for (var k = 0; k < 5; k++) {
                                            if (tracks[k] != undefined) {
                                                tracks[k].genres = artist.genres;
                                                tracks[k].nextEventId = event.id;
                                                collectedTracks.push(tracks[k])
                                            }
                                        }
                                        if (collectedTracks.length > 0) {
                                            factory.eventsGenrePlaylist.playlist.tracks = $filter('orderBy')(collectedTracks, 'confidence', true);
                                            factory.eventsGenrePlaylist.geopoint = $rootScope.geoLoc;
                                            factory.eventsGenrePlaylist.genre = genre;
                                            factory.eventsGenrePlaylist.playlist.name = 'Playlist des événements ' + genre;
                                            deferred.notify(angular.copy(factory.eventsGenrePlaylist.playlist))
                                        }
                                    })
                                })
                            }
                            clearTimeout(timeOut);
                            timeOut = setTimeout(function() {
                                deferred.resolve(factory.eventsPlaylist.playlist)
                            }, time)
                        });
                    })
                }
                return deferred.promise;
            }
        };
        return factory;

}]);