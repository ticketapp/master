app.controller("CarouselCtrl", function($scope, $timeout, $http, $sce, $localStorage, $rootScope, ArtistsFactory){
   $scope.infos=[];
    function removeAnimations() {
        for (var j = 0; j < $scope.infos.length; j++) {
            console.log($scope.infos[j])
            for (var k = 0; k < $localStorage.removedInfosMsg.length; k++) {
                if ($scope.infos[j].id == $localStorage.removedInfosMsg[k]) {
                    $scope.infos[j].animation = '';
                    console.log($localStorage.removedInfosMsg[k])
                }
            }
        }
    }
    var i = -1 ;
    var changeInf;
    function removeAnimation (i) {
        $localStorage.removedInfosMsg.push($scope.infos[i].id);
        $scope.infos[i].animation = '';
        $scope.elementEnCours.animation = '';
        console.log($localStorage.removedInfosMsg)
    }
    function updateInfo(){
        if(i === $scope.infos.length - 1){
            i = 0;
        } else {
            i++;
        }
        $scope.elementEnCours = {};
        $timeout(function () {
            $scope.$apply(function () {
                $scope.elementEnCours = $scope.infos[i];
            })
        }, 0);
        $scope.removeAnimation = function () {
            removeAnimation(i)
        };
        changeInf = $timeout(updateInfo,8000);
    }
    $http.get('/infos').success(function (data, status, headers, config) {
       //$scope.infos = data;

    }).error(function (data, status, headers, config) {
    });
    $scope.infos.push({
        id: 2,
        displayIfConnected: true,
        animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">vous pouvez ' +
            'trouver la rubrique FAQ/rapporter un bug ici.' +
            '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
            'transparent;top: 20px;' +
            'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
            'border-bottom: 10px solid transparent;' +
            'border-left: 10px solid white;"></div>'),
            style : 'right: 80px;padding: 10px;' +
                'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                'position: fixed;top: 170px;width: 25%;background: white;'

        },
        content: $sce.trustAsHtml('<h2 class="text-center textColorWhite margin10">Bienvenue</h2><p>' +
            '<b class="column large-6 large-offset-3 text-center textColorWhite medium-11">' +
            'Claude est en version beta, aidez le à s\'ammélliorez en rapportant vos bug ou en ' +
            'partageant vos suggestions.' +
            '</b>' +
            '</p>')
    });
    $scope.infos.push({
        id: 1,
        displayIfConnected: false,
        animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">' +
            'Connectez-vous en un clique via Facebook' +
            '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
            'transparent;top: 20px;' +
            'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
            'border-bottom: 10px solid transparent;' +
            'border-left: 10px solid white;"></div>'),
            style : 'right: 80px;padding: 10px;' +
                'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                'position: fixed;top: 230px;width: 25%;background: white;'

        },
        content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Connectez-vous</h3> <p>' +
            '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
            'Pour enrgistrer vos playlist et faire connaitre à Claude vos artistes et vos lieux favoris ' +
            '</b>' +
            '</p>')
    });
    removeAnimations();
    updateInfo();
    function getConnectedInfos () {
        if ($rootScope.connected == true) {
            $scope.infos = [
                {content: $sce.trustAsHtml('<h2 class="text-center textColorWhite margin10">Bienvenue</h2><p>' +
                    '<b class="column large-6 large-offset-3 text-center textColorWhite medium-11">' +
                    'Claude est en version beta, aidez le à s\'ammélliorez en rapportant vos bug ou en ' +
                    'partageant vos suggestions.' +
                    '</b>' +
                    '</p>')}
            ]
            function pushInfo(info, artist) {
                $scope.infos.push({content: $sce.trustAsHtml(info), artist: artist})
            }

            function getEventsArtist(artist) {
                ArtistsFactory.getArtistEvents(artist.facebookUrl).then(function (events) {
                    var info;
                    if (events.length > 0) {
                        var eventsLength;
                        if (events.length > 2) {
                            eventsLength = 2;
                        } else {
                            eventsLength = events.length;
                        }
                        info = '<h2 class="text-center textColorWhite margin10">' + artist.name + ' bientôt à :</h2>';
                        for (var e = 0; 0 < eventsLength; e++) {
                            info = info + '<a href="#/event/' + events[e].eventId + '" class="textColorWhite">' +
                                events[e].places[0].name + '</a>'
                        }
                        if (events.length > 2) {
                            info = info + '<a href="#/artist.facebookUrl" class="textColorWhite">' +
                                'Voir tous les événements</a>'

                        }
                        pushInfo(info, artist);
                    } else if (artist.tracks.length > 0) {
                        info = '<h2 class="text-center textColorWhite margin10"> Ecoutez vos musiques favorites et ' +
                            'enregistrer vos playlists avec Claude</h2>';
                        pushInfo(info, artist);
                    }
                });
            }

            ArtistsFactory.getFollowArtists().then(function (artists) {
                artists.forEach(getEventsArtist)
            })
        }
    }
    if ($rootScope.connected == true) {
        getConnectedInfos()
    } else {
        $rootScope.$watch('connected', function (newVal) {
            if (newVal == true) {
                getConnectedInfos();
            }
        })
    }
});