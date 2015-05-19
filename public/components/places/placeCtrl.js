angular.module('claudeApp').
    controller('PlaceCtrl', ['$scope', 'PlaceFactory', '$routeParams', 'WebsitesFactory',
        'RefactorGeopoint', '$rootScope',
        function ($scope, PlaceFactory, $routeParams, WebsitesFactory, RefactorGeopoint, $rootScope) {
            $scope.place = {};
            $scope.map = false;
            $scope.mapHeight = '100%';
            $scope.showDesc = false;
            PlaceFactory.getPlace($routeParams.id).then(function (place) {
                $scope.place = place;
                console.log(place);
                if ($scope.place.geographicPoint != undefined) {
                    $scope.geographicPoint =
                        RefactorGeopoint.refactorGeopoint($scope.place.geographicPoint);
                }
                if (place.websites != undefined) {
                    if (place.websites.indexOf(',') > -1) {
                        place.websites.split(',');
                    } else {
                        place.websites = [place.websites]
                    }
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(place.websites,
                        place.facebookUrl);
                }
                if ($rootScope.connected == true) {
                    PlaceFactory.getIsFollowed(place.placeId).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.isFollowed = isFollowed;
                        }
                    })
                } else {
                    $rootScope.$watch('connected', function () {
                        PlaceFactory.getIsFollowed(place.placeId).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                            }
                        })
                    })
                }
            });
            PlaceFactory.getPlaceEvents($routeParams.id).then(function (events) {
                $scope.place.events = events;
            });
            $scope.follow = function () {
                PlaceFactory.followPlaceByPlaceId($scope.place.placeId,
                    $scope.place.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = true;
                        }
                    })
            };

            $scope.stopFollow = function () {
                PlaceFactory.unfollowPlace($scope.place.placeId,
                    $scope.place.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = false;
                        }
                    })
            };
        }]);