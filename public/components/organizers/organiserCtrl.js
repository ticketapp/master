angular.module('claudeApp').
    controller('OrganizerCtrl', ['$scope', 'OrganizerFactory', '$routeParams', 'WebsitesFactory',
        'RefactorGeopoint',
        function ($scope, OrganizerFactory, $routeParams, WebsitesFactory, RefactorGeopoint) {
        $scope.organizer = {};
        $scope.map = false;
        $scope.showDesc = false;
        $scope.mapHeight = '100%';
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
        });
        OrganizerFactory.getOrganizerEvents($routeParams.id).then(function (events) {
            $scope.organizer.events = events;
        })
    }]);