angular.module('claudeApp').
controller('ArtistCtrl', ['$scope', '$localStorage', 'ArtistsFactory', '$timeout', '$filter',
        '$modal', '$rootScope', '$routeParams', 'WebsitesFactory', 'InfoModal',
    function ($scope, $localStorage, ArtistsFactory, $timeout, $filter, $modal, $rootScope,
              $routeParams, WebsitesFactory, InfoModal) {
        $scope.trackLimit = 12;
        $scope.trackTitle = '';
        $scope.showDesc = false;
        $scope.selectedTab = 0;
        $scope.isFollowed = false;
        $scope.showTop = false;
        $scope.numberOfTop = 0;
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
                        $timeout(function () {
                            $scope.$apply(function () {
                                $scope.tracks.push(track)
                            })
                        })
                    }
                }
            }

            var numberOfRates = 0;
            function countRates (track) {
                if (track.confidence != undefined && track.confidence > 5000000 && numberOfRates <50) {
                    numberOfRates ++;
                }
            }
            ArtistsFactory.getArtist($routeParams.facebookUrl).then(function (artist) {
                $scope.artist = artist;
                console.log(artist)
                $scope.tracks = [];
                artist.tracks = $filter('orderBy')(artist.tracks, 'confidence', true);
                artist.tracks.forEach(pushTrack);
                artist.tracks.forEach(countRates);
                $scope.numberOfTop = new Array(Math.round(numberOfRates/10));
                $scope.artist.tracks = $scope.tracks;
                $rootScope.loadingTracks = false;
                if (artist.websites != undefined) {
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(artist.websites,
                        $routeParams.facebookUrl);
                }
                if ($rootScope.connected == true) {
                    ArtistsFactory.getIsFollowed(artist.artistId).then(function (isFollowed) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                if (isFollowed == true || isFollowed == false) {
                                    $scope.isFollowed = isFollowed;
                                }
                            })
                        },0);
                    })
                }
                $rootScope.$watch('connected', function (connected) {
                    if (connected == false) {
                        $scope.isFollowed = false;
                    } else {
                        ArtistsFactory.getIsFollowed(artist.artistId).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $timeout(function () {
                                    $scope.$apply(function () {
                                        $scope.isFollowed = isFollowed;
                                    })
                                }, 0);
                            }
                        })
                    }
                })

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
            ArtistsFactory.followArtistByFacebookId($scope.artist.facebookId, $scope.artist.name).then(
                function (followed) {
                if (followed != 'error') {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.isFollowed = true;
                            InfoModal.displayInfo('Vous suivez ' + $scope.artist.name)
                        })
                    },0);
                }
            })
        };

        $scope.unfollow = function () {
            ArtistsFactory.unfollowArtist($scope.artist.artistId).then(function (followed) {
                if (followed != 'error') {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.isFollowed = false;
                            InfoModal.displayInfo('Vous ne suivez plus ' + $scope.artist.name)
                        })
                    },0);
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

        $scope.selectedTop = 0;
        $scope.selectTop = function (top) {
            $scope.selectedTop = top;
        };

        $scope.playTop = function () {
            if ($scope.selectedTop == 0) {
                $rootScope.addAndPlay($scope.artist.tracks, $scope.artist)
            } else {
                var tracksToPlay = [];
                for (var i = 0; i < $scope.selectedTop; i++) {
                    tracksToPlay.push($scope.tracks[i]);
                }
                $rootScope.addAndPlay(tracksToPlay, $scope.artist)
            }
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
                            InfoModal.displayInfo('Nous n\'avons pas trouvé "' + trackTitle + '"', 'error');
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
