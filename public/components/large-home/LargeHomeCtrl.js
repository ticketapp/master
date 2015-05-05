angular.module('claudeApp')
.controller('LargeHomeCtrl', ['$scope', '$localStorage', '$timeout', 'LargeHomeFactory', '$rootScope',
    function ($scope, $localStorage, $timeout, LargeHomeFactory, $rootScope) {

    $scope.infos = [];

    function getInfos () {
        LargeHomeFactory.getInfos().then(function (infos) {
            $scope.infos = infos;
        });
    }

    $rootScope.$watch('connected', function () {
        getInfos();
    });

    function removeAnimations() {
        if ($localStorage.removedInfosMsg != undefined) {
            for (var j = 0; j < $scope.infos.length; j++) {
                for (var k = 0; k < $localStorage.removedInfosMsg.length; k++) {
                    if ($scope.infos[j].id == $localStorage.removedInfosMsg[k]) {
                        $scope.infos[j].animation = '';
                        console.log($localStorage.removedInfosMsg[k])
                    }
                }
            }
        } else {
            $localStorage.removedInfosMsg = [];
        }
    }

    function pushInfoIdToLocalStorage(i) {
        $localStorage.removedInfosMsg.push($scope.infos[i].id);
    }
    $scope.pushInfoIdToLocalStorage = function (i) {
        pushInfoIdToLocalStorage(i)
    };

    function removeAnimation (i) {
        pushInfoIdToLocalStorage(i);
        $scope.infos[i].animation = '';
        $scope.elementEnCours.animation = '';
    }

    var i = -1;
    var updateInfo = setTimeout(function () {
        if (i === $scope.infos.length - 1) {
            i = 0;
        } else {
            i++;
        }

        $timeout(function () {
            $scope.$apply(function () {
                $scope.elementEnCours = $scope.infos[i];
            })
        }, 0);

        $scope.removeAnimation = function () {
            removeAnimation(i)
        };

    }, 8000);

    removeAnimations();
}]);