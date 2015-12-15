angular.module('claudeApp').factory('LargeHomeFactory', ['$http', '$q', '$rootScope', '$sce',
    'ArtistsFactory', 'RoutesFactory', 'OrganizerFactory', '$filter', 'PlaceFactory', 'FollowService', 'TrackService',
    function ($http, $q, $rootScope, $sce, ArtistsFactory, RoutesFactory, OrganizerFactory, $filter,
              PlaceFactory, FollowService, TrackService) {
    var factory = {
        infos : [],
        getInfos : function () {
            var deferred = $q.defer();
            if ($rootScope.connected === false) {
                   factory.infos = [
                       {
                           id: 40,
                           displayIfConnected: true,
                           animation: {content: $sce.trustAsHtml('<p style="color: white; text-align: center">' +
                               'Postez vos messages dans la rubrique bug/FAQ'),
                               style : 'right: 40px;padding: 10px;' +
                                   'position: fixed;top: 92px;width: 25%;'

                           },
                           content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Claude a besoin de vos suggestions</h3><p>' +
                               '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                               'Claude est en version Beta, aidez-le à s\'améliorer en reportant les bugs ou en laissant vos suggestions ' +
                               '</b>' +
                               '</p>')
                       },
                       {
                       id: 35,
                       displayIfConnected: false,
                       animation: {content: $sce.trustAsHtml('<p style="color: white; text-align: center">' +
                           'Connectez-vous en un clic via Facebook'),
                           style : 'right: 40px;padding: 10px;' +
                               'position: fixed;top: 150px;width: 25%;'

                       },
                       content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Connectez-vous</h3> <p>' +
                           '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                           'Pour enregistrer vos playlists et faire connaître à Claude vos artistes et vos lieux favoris ' +
                           '</b>' +
                           '</p>')
                   }
                   ];
                   deferred.resolve(factory.infos);
                return deferred.promise;
            } else if ($rootScope.connected === true) {
                factory.infos = [
                    {
                        id: 40,
                        displayIfConnected: true,
                        title: 'Claude a besoin de vos suggestions',
                        fixedTitle : false,
                        animation: {content: $sce.trustAsHtml('<p style="color: white; text-align: center">' +
                            'Postez vos messages dans la rubrique bug/FAQ'),
                            style : 'right: 40px;padding: 10px;' +
                            'position: fixed;top: 92px;width: 25%;'

                        },
                        content: $sce.trustAsHtml('<p>' +
                            '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                            'Claude est en version Beta, aidez-le à s\'améliorer en reportant les bugs ou en laissant vos suggestions ' +
                            '</b>' +
                            '</p>')
                    }
                ];
                function pushConnectedInfo(info, title, artist, fixedTitle) {
                    factory.infos.push({content: $sce.trustAsHtml(info), title: title, artist: artist, fixedTitle: fixedTitle})
                }

                // finish it when artist have have tracks

                function getEventsArtist(artist) {
                    ArtistsFactory.getArtistEvents(artist.facebookUrl).then(function (events) {
                        var info = '';
                        var title;
                        if (events.length > 0) {
                            var eventsLength;
                            if (events.length > 2) {
                                eventsLength = 2;
                            } else {
                                eventsLength = events.length;
                            }
                            title = artist.name + ' bientôt à :';
                            for (var e = 0; e < eventsLength; e++) {
                                info = info + '<div class="column large-12"><a style="font-size:25px; color: white;" href="#/events/' + events[e].eventId + '" class="textColorWhite">' +
                                    events[e].places[0].name + '</a></div>'
                            }
                            if (events.length > 2) {
                                info = info + '<a href="#/artist.facebookUrl" class="textColorWhite">' +
                                    'Voir tous les événements</a>'

                            }
                            pushConnectedInfo(info, title, artist, false);
                        } else if (artist.hasTracks) {
                            TrackService.getArtistTracks(artist.facebookUrl).then(function (tracks) {
                                tracks = tracks.map(function(track) {
                                    if (artist.genres) {
                                        track.genres = artist.genres
                                    } else {
                                        tracks.genres = []
                                    }
                                    return track;
                                });
                                artist.tracks = tracks;
                                title = 'Ecoutez vos musiques favorites et enregistrez vos playlists avec Claude';
                                pushConnectedInfo(info, title, artist, true);
                            });
                        }
                    });
                }

                function getOrganizerEvents (organizer) {
                    OrganizerFactory.getOrganizerEvents(organizer.id).then(function (events) {
                        events = $filter('orderBy')(events, 'startTime', false);
                        events.map(function(event) {
                            var timeout;
                            var time = 2000;
                            event.tracks = [];
                            if(event.artists) {
                                event.artists.map(function (artist) {
                                    TrackService.getArtistTracks(artist.facebookUrl).then(function (tracks) {
                                        for (var k = 0; k < 5; k++) {
                                            if (tracks[k] != undefined) {
                                                tracks[k].genres = artist.genres;
                                                event.tracks.push(tracks[k])
                                            }
                                        }
                                    });
                                    clearTimeout(timeout);
                                    timeout = setTimeout(function() {
                                        if (event.tracks.length > 0) {
                                            event.tracks = $filter('orderBy')(event.tracks, 'confidence', true);
                                            var title = 'Écouter la playlist des événements de ' + organizer.name + ' avec Claude';
                                            var info = '<a style="font-size:25px; color: white;" href="#/events/' + event.id + '">' +
                                                event.name + '</a>';
                                            var eventCopy = angular.copy(event);
                                            eventCopy.name = "la playlist de l'événement";
                                            pushConnectedInfo(info, title, eventCopy, true);
                                        }
                                    }, time)
                                });
                            }
                        });
                    })
                }

                function getPlaceEvents (place) {
                    PlaceFactory.getPlaceEvents(place.id).then(function (events) {
                        events = $filter('orderBy')(events, 'startTime', false);
                        events.map(function(event) {
                            var timeout;
                            var time = 2000;
                            event.tracks = [];
                            if(event.artists) {
                                event.artists.map(function (artist) {
                                    TrackService.getArtistTracks(artist.facebookUrl).then(function (tracks) {
                                        for (var k = 0; k < 5; k++) {
                                            if (tracks[k] != undefined) {
                                                tracks[k].genres = artist.genres;
                                                event.tracks.push(tracks[k])
                                            }
                                        }
                                    });
                                    clearTimeout(timeout);
                                    timeout = setTimeout(function() {
                                        if (event.tracks.length > 0) {
                                            event.tracks = $filter('orderBy')(event.tracks, 'confidence', true);
                                            var title = 'Ecouter la playlist des événements de ' + place.name + ' avec Claude';
                                            var info = '<a style="font-size:25px; color: white;" href="#/events/' + event.id + '">' +
                                                event.name + '</a>';
                                            var eventCopy = angular.copy(event);
                                            eventCopy.name = "la playlist de l'événement";
                                            pushConnectedInfo(info, title, eventCopy, true);
                                        }
                                    }, time)
                                });
                            }
                        });
                    })
                }

                FollowService.organizers.followed().then(function(organizers) {
                    organizers.forEach(getOrganizerEvents)
                });

                FollowService.places.followed().then(function(places) {
                    places.forEach(getPlaceEvents)
                });

                FollowService.artists.followed().then(function (artists) {
                    artists.forEach(getEventsArtist)
                });

                deferred.resolve(factory.infos);
                return deferred.promise;
            }
        }
    };
    return factory;
}]);