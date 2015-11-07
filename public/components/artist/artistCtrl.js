angular.module('claudeApp').
controller('ArtistCtrl', ['$scope', '$localStorage', 'ArtistsFactory', '$timeout', '$filter',
        '$modal', '$rootScope', '$routeParams', 'WebsitesFactory', 'InfoModal', 'UserFactory', 'TrackService', '$q',
    function($scope, $localStorage, ArtistsFactory, $timeout, $filter, $modal, $rootScope,
              $routeParams, WebsitesFactory, InfoModal, UserFactory, TrackService, $q) {

        $scope.trackLimit = 12;
        $scope.trackTitle = '';
        $scope.showDesc = false;
        $scope.selectedTab = 0;
        $scope.isFollowed = false;
        $scope.showTop = false;
        $scope.numberOfTop = 0;
        $scope.events = [];
        $scope.tracks = [];
        $scope.artist = [];
        $scope.artist.events = [];
        $rootScope.loadingTracks = true;
        var numberOfRates = 0;

        if ($localStorage.tracksSignaled == undefined) {
            $localStorage.tracksSignaled = [];
        }

        if ($rootScope.connected == true) {
            UserFactory.getRemovedTracks().then(function(uuids) {
                var tracksLength = uuids.length;
                for (var i = 0; i < tracksLength; i++) {
                    $localStorage.tracksSignaled.push(uuids[i])
                }
            })
        }

        function normalizeWebsites(artist) {
            if (artist.websites !== undefined) {
                $scope.websites = WebsitesFactory.normalizeWebsitesObject(artist.websites,
                    $routeParams.facebookUrl);
            }
        }

        function getIsFollowed(artist) {
            ArtistsFactory.getIsFollowed(artist.id).then(function(isFollowed) {
                $timeout(function() {
                    $scope.$apply(function() {
                        if (isFollowed === true || isFollowed === false) {
                            $scope.isFollowed = isFollowed;
                        }
                    })
                }, 0);
            })
        }

        function getArtistsEvent() {
            ArtistsFactory.getArtistEvents($routeParams.facebookUrl).then(function(events) {
                $scope.events = events;
                if (events.length === 0) {
                    $timeout(function() {
                        $scope.$apply(function() {
                            $scope.selectedTab = 1;
                        })
                    }, 0)
                }
            })
        }

        ArtistsFactory.getArtist($routeParams.facebookUrl).then(function(artist) {
            $timeout(function() {
                $scope.$apply(function() {
                    $scope.artist = artist;
                    $scope.tracks = TrackService.filterSignaledTracks(artist.tracks);
                    numberOfRates = TrackService.countRates($scope.tracks);
                    $scope.numberOfTop = new Array(Math.round(numberOfRates / 10));
                    TrackService.setFavorite($scope.tracks).then(function(tracksWithFavorites) {
                        $scope.tracks = tracksWithFavorites;
                        $scope.artist.tracks = $scope.tracks;
                        $rootScope.loadingTracks = false;
                    });
                    $rootScope.$watch("loadingTracks", function() {
                        $timeout(function() {
                            $scope.$apply(function() {
                                $scope.tracks = angular.copy(TrackService.filterSignaledTracks($scope.artist.tracks));
                            })
                        }, 0)
                    });
                })
            }, 0);

            normalizeWebsites(artist);
            getIsFollowed(artist);
            getArtistsEvent();

            $rootScope.$watch('connected', function(connected) {
                if (connected === false) {
                    $scope.isFollowed = false;
                } else {
                    ArtistsFactory.getIsFollowed(artist.artistId).then(function(isFollowed) {
                        if (isFollowed === true || isFollowed === false) {
                            $timeout(function() {
                                $scope.$apply(function() {
                                    $scope.isFollowed = isFollowed;
                                })
                            }, 0);
                        }
                    })
                }
            });

        });

        $scope.follow = function() {
            ArtistsFactory.followArtistByFacebookId($scope.artist.facebookId, $scope.artist.name).then(
                function(followed) {
                if (followed != 'error') {
                    $timeout(function() {
                        $scope.$apply(function() {
                            $scope.isFollowed = true;
                            InfoModal.displayInfo('Vous suivez ' + $scope.artist.name)
                        })
                    }, 0);
                }
            })
        };

        $scope.unfollow = function() {
            ArtistsFactory.unfollowArtist($scope.artist.id).then(function(followed) {
                if (followed != 'error') {
                    $timeout(function() {
                        $scope.$apply(function() {
                            $scope.isFollowed = false;
                            InfoModal.displayInfo('Vous ne suivez plus ' + $scope.artist.name)
                        })
                    }, 0);
                }
            })
        };

        $scope.closeTrack = function(index, trackId) {
            for (var i = 0; i < $scope.tracks.length; i++) {
                if ($scope.tracks[i].uuid === trackId) {
                    $scope.tracks.splice(i, 1);
                }
            }
            $scope.artist.tracks.splice(index, 1);
        };

        $scope.filterTracks = function() {
            $timeout(function() {
                $scope.$apply(function(){
                    $scope.artist.tracks = $filter('filter')($scope.tracks,
                        {title: $scope.trackTitle})
                })
            }, 0)
        };

        $scope.selectedTop = 0;
        $scope.selectTop = function(top) {
            $scope.selectedTop = top;
        };

        $scope.playTop = function() {
            if ($scope.selectedTop === 0) {
                $rootScope.addAndPlay($scope.artist.tracks, $scope.artist)
            } else {
                var tracksToPlay = [];
                for (var i = 0; i < $scope.selectedTop; i++) {
                    tracksToPlay.push($scope.tracks[i]);
                }
                $rootScope.addAndPlay(tracksToPlay, $scope.artist)
            }
        };

        $scope.suggestQuery = function(trackTitle, artistName, artistFacebookUrl) {
            $scope.suggest = false;
            if (trackTitle.length > 2) {
                artistName = ArtistsFactory.refactorArtistName(artistName);
                ArtistsFactory.getNewArtistTrack(artistName, trackTitle, artistFacebookUrl).then(
                    function(tracks) {
                        for (var i = 0; i < tracks.length; i++) {
                            if ($filter('filter')($scope.tracks, tracks[i].url, 'url').length === 0) {
                                tracks[i].genres = $scope.artist.genres;
                                $scope.tracks.push(tracks[i]);
                                $timeout(function() {
                                    $scope.$apply(function(){
                                        $scope.artist.tracks = $filter('filter')($scope.tracks,
                                            {title: $scope.trackTitle})
                                    })
                                }, 0)
                            }
                        }
                        if (tracks.length === 0) {
                            InfoModal.displayInfo('Nous n\'avons pas trouvé "' + trackTitle + '"', 'error');
                        }
                    }
                );
            } else {
                InfoModal.displayInfo('Le nom du morceau doit faire plus de deux lettres.');
            }
        };

        $scope.filterTracks = function(trackTitle) {
            $scope.trackTitle = trackTitle;
            $timeout(function() {
                $scope.$apply(function(){
                    $scope.artist.tracks = $filter('filter')($scope.tracks, {title: $scope.trackTitle})
                })
            }, 0)
        };

}]);
