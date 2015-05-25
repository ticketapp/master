angular.module('claudeApp').
    controller('EventCtrl', ['$scope', 'EventsFactory', '$routeParams', 'RefactorGeopoint',
        '$rootScope', 'UserFactory', 'InfoModal',
        function ($scope, EventFactory, $routeParams, RefactorGeopoint, $rootScope, UserFactory,
                  InfoModal) {
            $scope.event = {};
            $scope.map = false;
            $scope.isFollowed = false;
            EventFactory.getEvent($routeParams.id).then(function (event) {
                $scope.event = event;
                if (event.places[0] !== undefined && event.places[0].geographicPoint !== undefined) {
                    $scope.geographicPoint =
                        RefactorGeopoint.refactorGeopoint(event.places[0].geographicPoint);
                    UserFactory.getIsFollowedPlace(event.places[0].placeId).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.event.places[0].isFollowed = isFollowed
                        }
                    });
                    console.log(event.places[0].geographicPoint);
                    $scope.adresses = true;
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                } else if (event.geographicPoint !== undefined) {
                    $scope.geographicPoint = RefactorGeopoint.refactorGeopoint(event.geographicPoint);
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                }

                function isFollowedOrganizer(i) {
                    UserFactory.getIsFollowedOrganizer(event.organizers[i].organizerId).then(
                        function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                event.organizers[i].isFollowed = isFollowed
                            } else {
                                event.organizers[i].isFollowed = false;
                            }
                        }
                    )
                }

                function isFollowedArtists(i) {
                    UserFactory.ArtistIsFollowed(event.artists[i].artistId).then(
                        function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                event.artists[i].isFollowed = isFollowed
                            } else {
                                event.artists[i].isFollowed = false;
                            }
                        }
                    )
                }

                if ($rootScope.connected == true) {
                    EventFactory.getIsFollowed(event.eventId).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.isFollowed = isFollowed;
                            console.log(isFollowed)
                        }
                    });
                    if (event.organizers != undefined && event.organizers.length > 0) {
                        for (var i = 0; i < event.organizers.length; i++) {
                            isFollowedOrganizer(i);
                        }
                    }

                    if (event.artists != undefined && event.artists.length > 0) {
                        for (var j = 0; j < event.artists.length; j++) {
                            isFollowedArtists(j);
                        }
                    }
                }
                $rootScope.$watch('connected', function (connected) {
                    if (connected == false) {
                        $scope.isFollowed = false;
                    } else {
                        EventFactory.getIsFollowed(event.eventId).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                            }
                        })
                    }
                })
                if (event.organizers != undefined && event.organizers.length > 0) {
                    for (var i = 0; i < event.organizers.length; i++) {
                        isFollowedOrganizer(i);
                    }
                }

                if (event.artists != undefined && event.artists.length > 0) {
                    for (var j = 0; j < event.artists.length; j++) {
                        isFollowedArtists(j);
                    }
                }
            });
            $scope.follow = function () {
                EventFactory.followEventByEventId($scope.event.eventId, $scope.event.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = true;
                            InfoModal.displayInfo('Vous suivez ' + $scope.event.name)
                        }
                    })
            };

            $scope.unfollow = function () {
                EventFactory.unfollowEvent($scope.event.eventId, $scope.event.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = false;
                            InfoModal.displayInfo('Vous ne suivez plus ' + $scope.event.name)
                        }
                    })
            };
    }]);