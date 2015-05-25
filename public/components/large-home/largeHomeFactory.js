angular.module('claudeApp').factory('LargeHomeFactory', ['$http', '$q', '$rootScope', '$sce',
    'ArtistsFactory', 'RoutesFactory', 'OrganizerFactory', '$filter', 'PlaceFactory',
    function ($http, $q, $rootScope, $sce, ArtistsFactory, RoutesFactory, OrganizerFactory, $filter,
              PlaceFactory) {
    var factory = {
        infos : [],
        getInfos : function () {
            var deferred = $q.defer();
            if ($rootScope.connected === false) {
                   factory.infos = [
                       {
                           id: 20,
                           displayIfConnected: true,
                           animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">' +
                               'Poster vos messages dans la rubrique bug/FAQ' +
                               '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
                               'transparent;top: 20px;' +
                               'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
                               'border-bottom: 10px solid transparent;' +
                               'border-left: 10px solid white;"></div>'),
                               style : 'right: 80px;padding: 10px;' +
                                   'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                                   'position: fixed;top: 110px;width: 25%;background: white;'

                           },
                           content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Claude a besoin de vos suggestions</h3><p>' +
                               '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                               'Claude est en version Beta, aidez-le à s\'ammeliorer en reportant les bugs ou en laissant vos suggestions ' +
                               '</b>' +
                               '</p>')
                       },
                       {
                       id: 10,
                       displayIfConnected: false,
                       animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">' +
                           'Connectez-vous en un clic via Facebook' +
                           '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
                           'transparent;top: 20px;' +
                           'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
                           'border-bottom: 10px solid transparent;' +
                           'border-left: 10px solid white;"></div>'),
                           style : 'right: 80px;padding: 10px;' +
                               'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                               'position: fixed;top: 180px;width: 25%;background: white;'

                       },
                       content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Connectez-vous</h3> <p>' +
                           '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                           'Pour enregistrer vos playlists et faire connaitre à Claude vos artistes et vos lieux favoris ' +
                           '</b>' +
                           '</p>')
                   }
                   ];
                   deferred.resolve(factory.infos);
                return deferred.promise;
            } else if ($rootScope.connected === true) {
                factory.infos = [
                    {
                        id: 10,
                        displayIfConnected: true,
                        title: 'Claude a besoin de vos suggestions',
                        fixedTitle : false,
                        animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">' +
                            'Poster vos messages dans la rubrique bug/FAQ' +
                            '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
                            'transparent;top: 20px;' +
                            'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
                            'border-bottom: 10px solid transparent;' +
                            'border-left: 10px solid white;"></div>'),
                            style : 'right: 80px;padding: 10px;' +
                                'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                                'position: fixed;top: 110px;width: 25%;background: white;'

                        },
                        content: $sce.trustAsHtml('<p>' +
                            '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                            'Claude est en version Beta, aidez-le à s\'ammeliorer en reportant les bugs ou en laissant vos suggestions ' +
                            '</b>' +
                            '</p>')
                    }
                ];
                function pushConnectedInfo(info, title, artist, fixedTitle) {
                    factory.infos.push({content: $sce.trustAsHtml(info), title: title, artist: artist, fixedTitle: fixedTitle})
                }

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
                                info = info + '<div class="column large-12"><a href="#/events/' + events[e].eventId + '" class="textColorWhite">' +
                                    events[e].places[0].name + '</a></div>'
                            }
                            if (events.length > 2) {
                                info = info + '<a href="#/artist.facebookUrl" class="textColorWhite">' +
                                    'Voir tous les événements</a>'

                            }
                            pushConnectedInfo(info, title, artist, false);
                        } else if (artist.tracks.length > 0) {
                            title = 'Ecoutez vos musiques favorites et enregistrez vos playlists avec Claude';
                            pushConnectedInfo(info, title, artist, true);
                        }
                    });
                }
                ArtistsFactory.getFollowArtists().then(function (artists) {
                    artists.forEach(getEventsArtist)
                });

                function getOrganizerEvents (organizer) {
                    OrganizerFactory.getOrganizerEvents(organizer.organizerId).then(function (events) {
                        events = $filter('orderBy')(events, 'startTime', false);
                        var eventsLength = events.length;
                        for (var i = 0; i < eventsLength; i ++) {
                            if (events[i].artists.length > 0) {
                                events[i].tracks = [];
                                for (var j = 0; j < events[i].artists.length; j++) {
                                    for (var k = 0; k < 5; k++) {
                                        if (events[i].artists[j].tracks[k] != undefined) {
                                            events[i].tracks.push(events[i].artists[j].tracks[k])
                                        }
                                    }
                                }
                                if (events[i].tracks.length > 0) {
                                    events[i].tracks = $filter('orderBy')(events[i].tracks, 'confidence', true);
                                    var title = 'Ecouter la playliste des événements de ' + organizer.name + ' avec Claude';
                                    var info = '<a style="font-size:25px; color: white;" href="#/events/' + events[i].eventId +'">'+ events[i].name + '</a>';
                                    events[i].name = 'La playlist de l\'événements';
                                    console.log(events[i]);
                                    pushConnectedInfo(info, title, events[i], true);
                                }
                            }
                        }
                    })
                }

                function getPlaceEvents (place) {
                    PlaceFactory.getPlaceEvents(place.placeId).then(function (events) {
                        events = $filter('orderBy')(events, 'startTime', false);
                        var eventsLength = events.length;
                        for (var i = 0; i < eventsLength; i ++) {
                            if (events[i].artists.length > 0) {
                                events[i].tracks = [];
                                for (var j = 0; j < events[i].artists.length; j++) {
                                    for (var k = 0; k < 5; k++) {
                                        if (events[i].artists[j].tracks[k] != undefined) {
                                            events[i].tracks.push(events[i].artists[j].tracks[k])
                                        }
                                    }
                                }
                                if (events[i].tracks.length > 0) {
                                    events[i].tracks = $filter('orderBy')(events[i].tracks, 'confidence', true);
                                    var title = 'Ecouter les playliste des événements de ' + place.name + ' avec Claude';
                                    var info = '<a style="font-size:25px; color: white;" href="#/events/' + events[i].eventId +'">'+ events[i].name + '</a>';
                                    events[i].name = 'La playlist de l\'événements';
                                    pushConnectedInfo(info, title, events[i], true);
                                }
                            }
                        }
                    })
                }

                $http.get(RoutesFactory.organizers.getFollowedOrganizers()).
                    success(function (organizers) {
                        organizers.forEach(getOrganizerEvents)
                });

                $http.get(RoutesFactory.places.getFollowedPlaces()).
                    success(function (places) {
                        places.forEach(getPlaceEvents)
                });
                deferred.resolve(factory.infos);
                return deferred.promise;
            }
        }
    };
    return factory;
}]);