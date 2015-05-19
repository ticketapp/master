angular.module('claudeApp').
    controller('OrganizerCtrl', ['$scope', 'OrganizerFactory', '$routeParams', 'WebsitesFactory',
        'RefactorGeopoint', '$rootScope',
        function ($scope, OrganizerFactory, $routeParams, WebsitesFactory, RefactorGeopoint, $rootScope) {
        $scope.organizer = {};
        $scope.map = false;
        $scope.showDesc = false;
        $scope.mapHeight = '100%';
        $scope.isFollowed = false;
        OrganizerFactory.getOrganizer($routeParams.id).then(function (organizer) {
            $scope.organizer = organizer;
            if ($scope.organizer.geographicPoint != undefined) {
                $scope.geographicPoint =
                    RefactorGeopoint.refactorGeopoint($scope.organizer.geographicPoint);
            }
            if (organizer.websites != undefined) {
                if (organizer.websites.indexOf(',') > -1) {
                    organizer.websites.split(',');
                } else {
                    organizer.websites = [organizer.websites]
                }
                $scope.websites = WebsitesFactory.normalizeWebsitesObject(organizer.websites,
                    organizer.facebookUrl);
            }
            if ($rootScope.connected == true) {
                OrganizerFactory.getIsFollowed(organizer.organizerId).then(function (isFollowed) {
                    if (isFollowed == true || isFollowed == false) {
                        $scope.isFollowed = isFollowed;
                    }
                })
            } else {
                $rootScope.$watch('connected', function () {
                    OrganizerFactory.getIsFollowed(organizer.organizerId).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                            }
                        }
                    })
                })
            }
        });
        OrganizerFactory.getOrganizerEvents($routeParams.id).then(function (events) {
            $scope.organizer.events = events;
        });
        $scope.follow = function () {
            OrganizerFactory.followOrganizerByOrganizerId($scope.organizer.organizerId, $scope.organizer.name).
                then(
                function (followed) {
                    if (followed != 'error') {
                        $scope.isFollowed = true;
                    }
                })
        };

        $scope.stopFollow = function () {
            OrganizerFactory.unfollowOrganizer($scope.organizer.organizerId, $scope.organizer.name).
                then(
                function (followed) {
                    if (followed != 'error') {
                        $scope.isFollowed = false;
                    }
                })
        };
    }]);