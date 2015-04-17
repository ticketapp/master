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
                if (connectWin.document.getElementById('top').getAttribute("ng-init") == '$root.connected = true') {
                    $rootScope.passConnectedToTrue();
                    connectWin.close();
                    if ($rootScope.lastReq != {}) {
                        if ($rootScope.lastReq.method == 'post') {
                            if ($rootScope.lastReq.object != "") {
                                $http.post($rootScope.lastReq.path, $rootScope.lastReq.object).
                                    success(function (data) {
                                        $scope.info = $rootScope.lastReq.success;
                                        var modalInstance = $modal.open({
                                            templateUrl: 'assets/partials/_infoModal.html',
                                            controller: 'infoModalCtrl',
                                            resolve: {
                                                info: function () {
                                                    return $scope.info;
                                                }
                                            }
                                        });
                                        modalInstance.result.then(function () {
                                            $log.info('Modal dismissed at: ' + new Date());
                                        });
                                        $rootScope.lastReq = {};
                                    }).
                                    error(function (data) {
                                        console.log(data)
                                    })
                            } else {
                                $http.post($rootScope.lastReq.path).
                                    success(function (data) {
                                        $scope.info = $rootScope.lastReq.success;
                                        var modalInstance = $modal.open({
                                            templateUrl: 'assets/partials/_infoModal.html',
                                            controller: 'infoModalCtrl',
                                            resolve: {
                                                info: function () {
                                                    return $scope.info;
                                                }
                                            }
                                        });
                                        modalInstance.result.then(function () {
                                            $log.info('Modal dismissed at: ' + new Date());
                                        });
                                        $rootScope.lastReq = {};
                                    }).
                                    error(function (data) {
                                        console.log(data)
                                    })
                            }
                        }
                    }
                }
            }
        }, 1000);
    };/*
    document.getElementsByTagName('iframe')[0].onload = function () {
        if (document.getElementsByTagName('iframe')[0].contentWindow.location.href == 'http://localhost:9000/#/') {
            if (document.getElementsByTagName('iframe')[0].contentWindow.document.getElementById('top').getAttribute("ng-init") == '$root.connected = true') {
                $rootScope.passConnectedToTrue();
                console.log($rootScope.lastReq);
                if ($rootScope.lastReq != {}) {
                    if ($rootScope.lastReq.method == 'post') {
                        $http.post($rootScope.lastReq.path, $rootScope.lastReq.object).
                            success(function (data) {
                                console.log(data);
                                $rootScope.lastReq = {};
                            }).
                            error(function (data) {
                                console.log(data)
                            })
                    }
                }
            }
        }
    }*/
});