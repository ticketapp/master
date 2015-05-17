angular.module('claudeApp').
    controller('EventCtrl', ['$scope', 'EventsFactory', '$routeParams', 'RefactorGeopoint', '$rootScope',
        function ($scope, EventFactory, $routeParams, RefactorGeopoint, $rootScope) {
            $scope.event = {};
            $scope.map = false;
            EventFactory.getEvent($routeParams.id).then(function (event) {
                $scope.event = event;
                if (event.places.length > 0) {
                    $scope.geographicPoint =
                        RefactorGeopoint.refactorGeopoint(event.places[0].geographicPoint);
                    console.log($scope.geographicPoint);
                    $scope.adresses = true;
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                }
                if ($rootScope.connected == true) {
                    EventFactory.getIsFollowed(event.eventId).then(function (isFollowed) {
                        $scope.isFollowed = isFollowed;
                        console.log(isFollowed)
                    })
                } else {
                    $rootScope.$watch('connected', function () {
                        EventFactory.getIsFollowed(event.eventId).then(function (isFollowed) {
                            $scope.isFollowed = isFollowed;
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
                /*ArtistsFactory.followArtistByArtistId($scope.artist.artistId).then(function (followed) {
                 })*/
            };
    }]);