angular.module('claudeApp').
    controller('OrganizerCtrl', ['$scope', 'OrganizerFactory', '$routeParams', 'WebsitesFactory','$rootScope', 'InfoModal',
        function ($scope, OrganizerFactory, $routeParams, WebsitesFactory, $rootScope,
            InfoModal) {
        $scope.organizer = {};
        $scope.map = false;
        $scope.showDesc = false;
        $scope.mapHeight = '100%';
        $scope.isFollowed = false;
        OrganizerFactory.getOrganizer($routeParams.id).then(function (organizer) {
            $scope.organizer = organizer;
            if (organizer.description) {
                $scope.description = organizer.description.trim();
            }
            if ($scope.organizer.geographicPoint !== undefined) {
                $scope.geographicPoint = $scope.organizer.geographicPoint;
                $scope.map = true;
            }
            if (organizer.websites !== undefined) {
                if (organizer.websites.indexOf(',') > -1) {
                    organizer.websites.split(',');
                } else {
                    organizer.websites = [organizer.websites]
                }
                $scope.websites = WebsitesFactory.normalizeWebsitesObject(organizer.websites,
                    organizer.facebookId);
            } else {
                $scope.websites = WebsitesFactory.normalizeWebsitesObject([],
                    organizer.facebookId);
            }
            /*
            if (organizer.placeId !== undefined) {
                OrganizerFactory.getPlaceEvents(organizer.placeId).then(function (events) {
                    console.log(events);
                    if ($scope.organizer.events) {
                        $scope.organizer.events = $scope.organizer.events.concat(events)
                    } else {
                        $scope.organizer.events = events;
                    }
                })
            }*/
            if ($rootScope.connected === true) {
                OrganizerFactory.getIsFollowed(organizer.id).then(function (isFollowed) {
                    if (isFollowed === true || isFollowed === false) {
                        $scope.isFollowed = isFollowed;
                    }
                })
            }
            $rootScope.$watch('connected', function (connected) {
                if (connected === false) {
                    $scope.isFollowed = false;
                } else {
                    OrganizerFactory.getIsFollowed(organizer.id).then(function (isFollowed) {
                        if (isFollowed === true || isFollowed === false) {
                            if (isFollowed === true || isFollowed === false) {
                                $scope.isFollowed = isFollowed;
                            }
                        }
                    })
                }
            });
        });
        OrganizerFactory.getOrganizerEvents($routeParams.id).then(function (events) {
            $scope.events = events;
        });
        OrganizerFactory.getPassedEvents($routeParams.id).then(function (events) {
            $scope.passedEvents = events;
        });
        $scope.follow = function () {
            OrganizerFactory.followOrganizerByOrganizerId($scope.organizer.id, $scope.organizer.name).
                then(
                function (followed) {
                    if (followed !== 'error') {
                        $scope.isFollowed = true;
                        InfoModal.displayInfo('Vous suivez ' + $scope.organizer.name)
                    }
                })
        };

        $scope.unfollow = function () {
            OrganizerFactory.unfollowOrganizer($scope.organizer.id, $scope.organizer.name).
                then(
                function (followed) {
                    if (followed !== 'error') {
                        $scope.isFollowed = false;
                        InfoModal.displayInfo('Vous ne suivez plus ' + $scope.organizer.name)
                    }
                })
        };
    }]);