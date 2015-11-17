angular.module('claudeApp').
    controller('PlaceCtrl', ['$scope', 'PlaceFactory', '$routeParams', 'WebsitesFactory', '$rootScope', 'InfoModal',
        function ($scope, PlaceFactory, $routeParams, WebsitesFactory, $rootScope,
            InfoModal) {
            $scope.place = {};
            $scope.map = false;
            $scope.mapHeight = '100%';
            $scope.showDesc = false;
            PlaceFactory.getPlace($routeParams.id).then(function (place) {
                $scope.place = place;
                if ($scope.place.address !== undefined && $scope.place.address.geographicPoint !== undefined) {
                    $scope.geographicPoint = $scope.place.address.geographicPoint;
                    $scope.map = true;
                } else if ($scope.place.geographicPoint !== undefined) {
                    $scope.geographicPoint = $scope.place.geographicPoint;
                    $scope.map = true;
                }
                if (place.websites != undefined) {
                    if (place.websites.indexOf(',') > -1) {
                        place.websites.split(',');
                    } else {
                        place.websites = [place.websites]
                    }
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(place.websites,
                        place.facebookId);
                } else {
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject([],
                        place.facebookId);
                }
                if ($rootScope.connected == true) {
                    PlaceFactory.getIsFollowed(place.id).then(function (isFollowed) {
                        if (isFollowed == true || isFollowed == false) {
                            $scope.isFollowed = isFollowed;
                        }
                    })
                }
                $rootScope.$watch('connected', function (connected) {
                    if (connected == false) {
                        $scope.isConnected = false;
                    } else {
                        PlaceFactory.getIsFollowed(place.id).then(function (isFollowed) {
                            if (isFollowed == true || isFollowed == false) {
                                $scope.isFollowed = isFollowed;
                            }
                        })
                    }
                })
            });
            PlaceFactory.getPlaceEvents($routeParams.id).then(function (events) {
                $scope.events = events;
            });
            PlaceFactory.getPassedEvents($routeParams.id).then(function (events) {
                $scope.passedEvents = events;
            });
            $scope.follow = function () {
                PlaceFactory.followPlaceByPlaceId($scope.place.id,
                    $scope.place.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = true;
                            InfoModal.displayInfo('Vous suivez ' + $scope.place.name)
                        }
                    })
            };

            $scope.unfollow = function () {
                PlaceFactory.unfollowPlace($scope.place.id,
                    $scope.place.name).then(
                    function (followed) {
                        if (followed != 'error') {
                            $scope.isFollowed = false;
                            InfoModal.displayInfo('Vous ne suivez plus ' + $scope.place.name)
                        }
                    })
            };
        }]);