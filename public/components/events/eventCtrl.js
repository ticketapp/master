angular.module('claudeApp').
    controller('EventCtrl', ['$scope', 'EventsFactory', '$routeParams', 'RefactorGeopoint',
        '$rootScope', 'UserFactory',
        function ($scope, EventFactory, $routeParams, RefactorGeopoint, $rootScope, UserFactory) {
            $scope.event = {};
            $scope.map = false;
            $scope.isFollowed = false;
            EventFactory.getEvent($routeParams.id).then(function (event) {
                $scope.event = event;
                if (event.places[0].geographicPoint != undefined) {
                    $scope.geographicPoint =
                        RefactorGeopoint.refactorGeopoint(event.places[0].geographicPoint);
                    UserFactory.getIsFollowedPlace(event.places[0]).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.event.places[0].isFollowed = isFollowed
                        }
                    });
                    console.log(event.places[0].geographicPoint);
                    $scope.adresses = true;
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

                if (event.organizers != undefined && event.organizers.length > 0) {
                    for (var i = 0; i < event.organizers.length; i++) {
                        isFollowedOrganizer(i);
                    }
                }
                if ($rootScope.connected == true) {
                    EventFactory.getIsFollowed(event.eventId).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.isFollowed = isFollowed;
                            console.log(isFollowed)
                        }
                    })
                } else {
                    $rootScope.$watch('connected', function () {
                        EventFactory.getIsFollowed(event.eventId).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                                console.log(isFollowed)
                            }
                        })
                    })
                }
            });
            $scope.follow = function () {
                EventFactory.followEventByEventId($scope.event.eventId, $scope.event.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = true;
                        }
                    })
            };

            $scope.stopFollow = function () {
                EventFactory.unfollowEvent($scope.event.eventId, $scope.event.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = false;
                        }
                    })
            };
    }]);