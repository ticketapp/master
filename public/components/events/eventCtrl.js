angular.module('claudeApp').
    controller('EventCtrl', ['$scope', 'EventsFactory', '$routeParams', 'RefactorGeopoint',
        function ($scope, EventFactory, $routeParams, RefactorGeopoint) {
            $scope.event = {};
            $scope.map = false;
            EventFactory.getEvent($routeParams.id).then(function (event) {
                $scope.event = event;
                if (event.places.length > 0) {
                    $scope.geographicPoint =
                        RefactorGeopoint.refactorGeopoint(event.addresses[0].geographicPoint);
                    $scope.adresses = true;
                    $scope.mapHeight = '300px';
                    $scope.map = true;
                }
            })
    }]);