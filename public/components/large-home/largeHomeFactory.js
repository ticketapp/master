angular.module('claudeApp').factory('LargeHomeFactory', ['$http', '$q', '$rootScope', '$sce', 'ArtistsFactory',
    function ($http, $q, $rootScope, $sce, ArtistsFactory) {
    var factory = {
        infos : [],
        getInfos : function () {
            var deferred = $q.defer();
            if ($rootScope.connected === false) {
                $http.get('/infos').success(function (data) {
                   function pushInfo (info) {
                       info.content = $sce.trustAsHtml(info.content);
                       info.animationContent = $sce.trustAsHtml(info.animationContent);
                       info.animationStyle = $sce.trustAsHtml(info.animationStyle);
                       factory.infos.push(info);
                   }
                   data.forEach(pushInfo);
                   deferred.resolve(factory.infos);
                }).error(function () {
                });
                return deferred.promise;
            } else if ($rootScope.connected === true) {
                $http.get('/infos').success(function (data) {

                    function pushInfo(info) {
                        if (info.displayIfConnected === true) {
                            info.content = $sce.trustAsHtml(info.content);
                            info.animationContent = $sce.trustAsHtml(info.animationContent);
                            info.animationStyle = $sce.trustAsHtml(info.animationStyle);
                            factory.infos.push(info);
                        }
                    }
                    data.forEach(pushInfo);
                    function pushConnectedInfo(info, title, artist, fixedTitle) {
                        factory.infos.push({content: $sce.trustAsHtml(info), title: title, artist: artist, fixedTitle: fixedTitle})
                    }

                    function getEventsArtist(artist) {
                        ArtistsFactory.getArtistEvents(artist.facebookUrl).then(function (events) {
                            var info = '';
                            var title;
                            console.log(events);
                            if (events.length > 0) {
                                var eventsLength;
                                if (events.length > 2) {
                                    eventsLength = 2;
                                } else {
                                    eventsLength = events.length;
                                }
                                title = '<h2 class="text-center column medium-11 textColorWhite margin10">' + artist.name + ' bientôt à :</h2>';
                                for (var e = 0; e < eventsLength; e++) {
                                    info = info + '<div class="column large-12"><a href="#/event/' + events[e].eventId + '" class="textColorWhite">' +
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
                    deferred.resolve(factory.infos);
                });
                return deferred.promise;
            }
        }
    };
    return factory;
}]);