angular.module('claudeApp').
    controller('EventCtrl', ['$scope', 'EventsFactory', '$routeParams', '$rootScope', 'UserFactory', 'InfoModal', 'FollowService',
        "TrackService",
        function ($scope, EventFactory, $routeParams, $rootScope, UserFactory, InfoModal, FollowService, TrackService) {
            $scope.event = {};
            $scope.map = false;
            $scope.isFollowed = false;
            EventFactory.getEvent($routeParams.id).then(function (event) {
                $scope.event = event;
                if (event.places[0] && event.places[0].geographicPoint) {
                    $scope.geographicPoint = event.places[0].geographicPoint;
                    FollowService.places.isFollowed(event.places[0].id).then(function (isFollowed) {
                        if (isFollowed === true || isFollowed === false) {
                            $scope.event.places[0].isFollowed = isFollowed
                        }
                    });
                    $scope.adresses = true;
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                } else if (event.geographicPoint) {
                    $scope.geographicPoint = event.geographicPoint;
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                } else if (event.addresses[0] && event.addresses[0].geographicPoint) {
                    $scope.geographicPoint = event.addresses[0].geographicPoint;
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                }
                function setIsFollowedOrganizer(organizer) {
                    FollowService.organizers.isFollowed(organizer.id).then(
                        function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                organizer.isFollowed = isFollowed
                            } else {
                                organizer.isFollowed = false;
                            }
                            return organizer
                        }
                    )
                }


                function setIsFollowedArtist(artist) {
                    FollowService.artists.isFollowed(artist.id).then(
                        function (isFollowed) {
                            if (isFollowed === true || isFollowed === false) {
                                artist.isFollowed = isFollowed
                            } else {
                                artist.isFollowed = false;
                            }
                            return artist
                        }
                    )
                }

                if ($rootScope.connected == true) {
                    FollowService.events.isFollowed(event.id).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.isFollowed = isFollowed;
                        }
                    });
                    if (event.organizers && event.organizers.length > 0) {
                        event.organizers.map(function(organizer) {
                            return setIsFollowedOrganizer(organizer)
                        });
                    }

                    if (event.artists && event.artists.length > 0) {
                        event.artists.map(function(artist) {
                            return setIsFollowedArtist(artist)
                        });
                    }
                }
                $rootScope.$watch('connected', function (connected) {
                    if (connected == false) {
                        $scope.isFollowed = false;
                    } else {
                        FollowService.events.isFollowed(event.id).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                            }
                        });
                    }
                });
                if (event.organizers != undefined && event.organizers.length > 0) {
                    event.organizers.map(function(organizer) {
                        return setIsFollowedOrganizer(organizer)
                    });
                }

                if (event.artists != undefined && event.artists.length > 0) {
                    event.artists.map(function(artist) {
                        return setIsFollowedArtist(artist)
                    });
                }
            });
            $scope.follow = function () {
                FollowService.events.follow($scope.event.id, $scope.event.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = true;
                            InfoModal.displayInfo('Vous suivez ' + $scope.event.name)
                        }
                    })
            };

            $scope.unfollow = function () {
                FollowService.events.unfollow($scope.event.id, $scope.event.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = false;
                            InfoModal.displayInfo('Vous ne suivez plus ' + $scope.event.name)
                        }
                    })
            };

            $scope.getTracksAndPlay = function(artist) {
                TrackService.getArtistTracks(artist.facebookUrl).then(function(tracks) {
                    tracks.map(function(track) {
                        track.genres = artist.genres;
                        return track;
                    });
                    $rootScope.addAndPlay(tracks, artist)
                })
            }
    }]);