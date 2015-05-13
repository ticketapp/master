angular.module('claudeApp').
controller('ArtistCtrl', ['$scope', '$localStorage', 'ArtistsFactory', '$timeout', '$filter',
        '$modal', '$rootScope', '$routeParams', 'WebsitesFactory', 'InfoModal',
    function ($scope, $localStorage, ArtistsFactory, $timeout, $filter, $modal, $rootScope,
              $routeParams, WebsitesFactory, InfoModal) {

        $scope.trackLimit = 12;
        $scope.trackTitle = '';
        $scope.showDesc = false;
        $scope.selectedTab = 0;
        if ($localStorage.tracksSignaled == undefined) {
            $localStorage.tracksSignaled = [];
        }
        if ($rootScope.artisteToCreate != true) {
            $scope.tracks = [];
            $scope.artist = [];
            $scope.artist.events = [];
            $rootScope.loadingTracks = true;
            function pushTrack (track) {
                var pushTrack = true;
                for (var i = 0; i < $localStorage.tracksSignaled.length; i++) {
                    if (track.trackId == $localStorage.tracksSignaled[i]) {
                        pushTrack = false;
                        return
                    }
                }
                if (pushTrack == true) {
                    $scope.tracks.push(track)
                }
            }
            ArtistsFactory.getArtist($routeParams.facebookUrl).then(function (artist) {
                $scope.artist = artist;
                $scope.tracks = [];
                artist.tracks.forEach(pushTrack);
                $scope.artist.tracks = $scope.tracks;
                $rootScope.loadingTracks = false;
                if (artist.websites != undefined) {
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(artist.websites,
                        $routeParams.facebookUrl);
                }
            });
            ArtistsFactory.getArtistEvents($routeParams.facebookUrl).then(function (events) {
                $scope.artist.events = events;
                if (events.length == 0) {
                    $scope.selectedTab = 1;
                }
            })
        } else {
            $scope.selectedTab = 1;
            ArtistsFactory.passArtisteToCreateToFalse();
        }

        if ($localStorage.tracksSignaled == undefined) {
            $localStorage.tracksSignaled = [];
        }

        $scope.follow = function () {
            ArtistsFactory.followArtistByArtistId($scope.artist.artistId).then(function (followed) {
                console.log(followed)
            })
        };

        $scope.filterTracks = function () {
            $timeout(function () {
                $scope.$apply(function(){
                    $scope.artist.tracks = $filter('filter')($scope.tracks,
                        {title: $scope.trackTitle})
                })
            }, 0)
        };

        $scope.suggestQuery = function (trackTitle, artistName, artistFacebookUrl) {
            $scope.suggest = false;
            if (trackTitle.length > 2) {
                artistName = artistName.toLowerCase().replace('officiel', '');
                artistName = artistName.toLowerCase().replace('official', '');
                artistName = artistName.toLowerCase().replace('music', '');
                artistName = artistName.toLowerCase().replace('musique', '');
                artistName = artistName.toLowerCase().replace('musik', '');
                artistName = artistName.toLowerCase().replace('fanpage', '');
                artistName = artistName.toLowerCase().replace(/[^\w\s].*/, '');
                console.log(artistName, trackTitle, artistFacebookUrl)
                ArtistsFactory.getNewArtistTrack(artistName, trackTitle, artistFacebookUrl).then(
                    function (tracks) {
                        for (var i = 0; i < tracks.length; i++) {
                            $scope.artist.tracks.push(tracks[i]);
                            $scope.tracks.push(tracks[i])
                        }
                        if (tracks.length == 0) {
                            InfoModal.displayInfo('Nous n\'avons pas trouvé "' + trackTitle + '"');
                        }
                    }
                );
            } else {
                InfoModal.displayInfo('le nom de la track doit faire plus de deux lettres');
            }
        };

        $scope.filterTracks = function (trackTitle) {
            $scope.trackTitle = trackTitle;
            $timeout(function () {
                $scope.$apply(function(){
                    $scope.artist.tracks = $filter('filter')($scope.tracks, {title: $scope.trackTitle})
                })
            }, 0)
        };

        $scope.signalTrack = function (index) {
            var modalInstance = $modal.open({
                template: '<form ng-submit="ok(reason)">' +
                    '<b class="column large-12 center">Pour quelle raison souhaitez-vous signaler ' +
                    'ce morceau ? <br/>'+
                    'Attention, en signalant ce morceau il sera supprimé des morceaux que Claude ' +
                    'vous proppose</b>' +
                    '<select ng-model="reason">'+
                    '<option value="B">Mauvais Artist</option>'+
                    '<option value="Q">Mauvaise qualitée</option>'+
                    '</select><b class="column large-12">{{error}}</b>'+
                    '<input type="submit" class="button">'+
                    '<a class="button float-right" ng-click="cancel()">Annuler</a>'+
                    '</form>',
                controller: 'SignalTrackCtrl',
                resolve: {
                    index: function () {
                        return index;
                    }
                }
            });
            modalInstance.result.then(function () {
                var tracksLength = $scope.tracks.length;
                for (var i = 0; i < tracksLength; i++) {
                    if ($scope.tracks[i].trackId == $scope.artist.tracks[index].trackId) {
                        $localStorage.tracksSignaled.push($scope.tracks[i].trackId);
                        $scope.tracks.splice(i, 1);
                        tracksLength --;
                    }
                }
                $scope.artist.tracks.splice(index, 1);
            }, function () {
            });

        }
}]);

angular.module('claudeApp').controller('SignalTrackCtrl', function ($scope, $modalInstance) {

    $scope.ok = function (reason) {
        if (reason != undefined) {
            $modalInstance.close();
        } else {
            $scope.error = 'veuyez renseigner ce champs'
        }
    };
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});