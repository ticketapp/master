angular.module('claudeApp').
    controller('MapCtrl', ['$scope', '$rootScope', '$timeout',
        function ($scope, $rootScope, $timeout) {
        $scope.zoom = 13;
        $scope.travelMode = 'DRIVING';
        $scope.moreZoom = function() {
            $scope.zoom = $scope.zoom + 1;
        };
        $scope.lessZoom = function() {
            $scope.zoom = $scope.zoom - 1;
        };
        $scope.getItineraire = function () {
            $scope.map = false;
            $scope.start = $rootScope.geoLoc.replace("(", "").replace(")", "").replace(",", ", ");
            $scope.map = true;
            var oldInf = window.directionInfos;
            var waitForInfosChanges = setInterval(function () {
                if (window.directionInfos != oldInf) {
                    clearInterval(waitForInfosChanges);
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.directionInfos = window.directionInfos;
                            console.log($scope.directionInfos)
                        })
                    }, 0)
                }
            }, 500);
        };
        $scope.changeTravelMode = function (travelMode) {
            $scope.map = false;
            $scope.travelMode = travelMode;
            $scope.map = true;
            var oldInf = window.directionInfos;
            var waitForInfosChanges = setInterval(function () {
                if (window.directionInfos != oldInf) {
                    clearInterval(waitForInfosChanges);
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.directionInfos = window.directionInfos;
                        })
                    }, 0)
                }
            }, 500);
        };
    }]);