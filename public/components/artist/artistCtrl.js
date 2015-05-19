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
                if ($rootScope.favoritesTracks) {
                    if ($rootScope.favoritesTracks.indexOf(track.trackId) > -1) {
                        track.isFavorite = true;
                    }
                }
                if ($localStorage.tracksSignaled) {
                    if ($localStorage.tracksSignaled.indexOf(track.trackId) == -1) {
                        $scope.tracks.push(track)
                    }
                }
            }
            ArtistsFactory.getArtist($routeParams.facebookUrl).then(function (artist) {
                $scope.artist = artist;
                $scope.tracks = [];
                artist.tracks = $filter('orderBy')(artist.tracks, 'confidence', true);
                artist.tracks.forEach(pushTrack);
                $scope.artist.tracks = $scope.tracks;
                $rootScope.loadingTracks = false;
                if (artist.websites != undefined) {
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(artist.websites,
                        $routeParams.facebookUrl);
                }
                if ($rootScope.connected == true) {
                    ArtistsFactory.getIsFollowed(artist.artistId).then(function (isFollowed) {
                        $scope.isFollowed = isFollowed;
                    })
                } else {
                    $rootScope.$watch('connected', function () {
                        ArtistsFactory.getIsFollowed(artist.artistId).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                            }
                        })
                    })
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
            ArtistsFactory.followArtistByArtistId($scope.artist.artistId, $scope.artist.name).then(
                function (followed) {
                if (followed != 'error') {
                    $scope.isFollowed = true;
                }
            })
        };

        $scope.stopFollow = function () {
            ArtistsFactory.unfollowArtist($scope.artist.artistId).then(function (followed) {
                if (followed != 'error') {
                    $scope.isFollowed = false;
                }
            })
        };

        $scope.closeTrack = function (index) {
            for (var i = 0; i < $scope.tracks.length; i++) {
                if ($scope.tracks[i].trackId == $scope.artist.tracks[index].trackId) {
                    $scope.tracks.splice(i, 1);
                }
            }
            $scope.artist.tracks.splice(index, 1);
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
                artistName = artistName.toLowerCase().replace(/[^\w\s-].*/, '');
                console.log(artistName, trackTitle, artistFacebookUrl);
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

}]);
