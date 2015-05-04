angular.module('claudeApp').
    controller('PlaceCtrl', ['$scope', 'PlaceFactory', '$routeParams', 'WebsitesFactory',
        'RefactorGeopoint',
        function ($scope, PlaceFactory, $routeParams, WebsitesFactory, RefactorGeopoint) {
            $scope.place = {};
            $scope.map = false;
            $scope.mapHeight = '100%';
            PlaceFactory.getPlace($routeParams.id).then(function (place) {
                $scope.place = place;
                if ($scope.place.geographicPoint != undefined) {
                    $scope.geographicPoint =
                        RefactorGeopoint.refactorGeopoint($scope.place.geographicPoint);
                }
                if (place.websites != undefined) {
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(place.websites,
                        place.facebookUrl);
                }
            });
            PlaceFactory.getPlaceEvents($routeParams.id).then(function (events) {
                $scope.place.events = events;
            })
        }]);