angular.module('claudeApp')
.controller('LargeHomeCtrl', ['$scope', '$localStorage', '$timeout', 'LargeHomeFactory', '$rootScope',
        '$location',
    function ($scope, $localStorage, $timeout, LargeHomeFactory, $rootScope, $location) {

    $scope.infos = [];
    $scope.animateMsg = true;
    function removeAnimations() {
        if ($localStorage.removedInfosMsg != undefined) {
            for (var j = 0; j < $scope.infos.length; j++) {
                if ($localStorage.removedInfosMsg.indexOf($scope.infos[j].id) > -1) {
                    $scope.infos[j].animation = '';
                }
            }
        } else {
            $localStorage.removedInfosMsg = [];
        }
    }

    function getInfos () {
        LargeHomeFactory.getInfos().then(function (infos) {
            $scope.infos = infos;
            removeAnimations();
            $scope.elementEnCours = $scope.infos[0];
        });
    }

    $rootScope.$watch('connected', function () {
        getInfos();
    });

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

    var i = 0;
    var updateInfo = setInterval(function () {
        if (i === $scope.infos.length - 1) {
            i = 0;
        } else {
            i++;
        }
        $timeout(function () {
            $scope.$apply(function () {
                $scope.animateMsg = false;
            })
        }, 0);
        $timeout(function () {
            $scope.$apply(function () {
                $scope.elementEnCours = $scope.infos[i];
                $scope.animateMsg = true;
            })
        }, 0);

        $scope.removeAnimation = function () {
            removeAnimation(i)
        };

    }, 8000);
    updateInfo;
    $scope.$on('$locationChangeSuccess', function () {
        clearInterval(updateInfo)
    })
}]);