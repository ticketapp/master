app.controller('connectCtrl', function ($scope, $rootScope, $http) {
    $scope.connectLink = function (url) {
        var connectWin = window.open(url, "", "toolbar=no, scrollbars=no, resizable=no, width=400, height=400")
        var connectWinPath = connectWin.location.href;
        var changePath = setInterval(function() {
            if (connectWin.location.href == undefined) {
                clearInterval(changePath)
            }
            if (connectWin.location.href == 'http://localhost:9000/#/') {
                console.log(connectWin.location.href);
                if (connectWin.document.getElementById('top').getAttribute("ng-init") == '$root.connected = true') {
                    $rootScope.passConnectedToTrue();
                    console.log($rootScope.lastReq);
                    connectWin.close();
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