app.controller('connectCtrl', function ($scope, $rootScope, $http, $modal) {
    $scope.connectByMail = function () {
        $http.post('/authenticate/userpass', {username: $scope.username, password: $scope.password}).
            success(function (data) {
                console.log(data)
            }).
            error(function (data) {
                console.log(data)
            })
    };
    $scope.connectLink = function (url) {
        var connectWin = window.open(url, "", "toolbar=no, scrollbars=no, resizable=no, width=500, height=500");
        var changePath = setInterval(function() {
            if (connectWin.location.href == undefined) {
                clearInterval(changePath)
            }
            if (connectWin.location.href == 'http://localhost:9000/#/connected') {
                clearInterval(changePath)
                function getConnected () {
                    if (connectWin.document.getElementById('top') != undefined || connectWin.document.getElementById('top') != 'null') {
                        if (connectWin.document.getElementById('top').getAttribute("ng-init") == '$root.connected = true') {
                            $rootScope.passConnectedToTrue();
                            connectWin.close();
                            if ($rootScope.lastReq != {}) {
                                if ($rootScope.lastReq.method == 'post') {
                                    if ($rootScope.lastReq.object != "") {
                                        $http.post($rootScope.lastReq.path, $rootScope.lastReq.object).
                                            success(function (data) {
                                                $scope.info = $rootScope.lastReq.success;
                                                $rootScope.lastReq = {};
                                            }).
                                            error(function (data) {
                                                if (data.error == 'Credentials required') {
                                                    $rootScope.storeLastReq('post', $rootScope.lastReq.path, $rootScope.lastReq.object, $rootScope.lastReq.success)
                                                } else {
                                                    console.log(data)
                                                    $scope.info = 'Désolé une erreur s\'est produite';
                                                }
                                            })
                                    } else {
                                        $http.post($rootScope.lastReq.path).
                                            success(function (data) {
                                                $scope.info = $rootScope.lastReq.success;
                                                $rootScope.lastReq = {};
                                            }).
                                            error(function (data) {
                                                if (data.error == 'Credentials required') {
                                                    $rootScope.storeLastReq('post', $rootScope.lastReq.path, $rootScope.lastReq.object, $rootScope.lastReq.success)
                                                } else {
                                                    console.log(data)
                                                    $scope.info = 'Désolé une erreur s\'est produite';
                                                }
                                            })
                                    }
                                }
                            }
                        }
                    } else {
                        getConnected();
                    }
                }
                getConnected();
            }
        }, 1000);
    };
});