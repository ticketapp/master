app.controller('connectCtrl', function ($scope, $rootScope, $http, $modal) {
    $scope.connectByMail = function () {
        $http.post('/authenticate/userpass', {username: $scope.username, password: $scope.password}).
            success(function (data) {

            }).
            error(function (data) {

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
                            function getFbPages (route) {
                                $http.get(route).
                                    success(function (data) {
                                        console.log(data)
                                        if (data.likes != undefined) {
                                            var pages = data.likes.data;
                                            var next = data.likes.paging.next
                                            var pagesLength = data.likes.data.length;
                                        } else {
                                            var pages = data.data;
                                            var next = data.paging.next;
                                            var pagesLength = data.data.length;
                                        }
                                        for (var i = 0; i < pagesLength; i++) {
                                            if (pages[i].category == 'Concert tour') {
                                                //folow organizer
                                            } else if (pages[i].category == "Musician/band") {
                                                //folowArtist
                                            } else if (pages[i].category == "concert venue") {
                                                //folowPlace
                                            }
                                        }
                                        getFbPages(next)
                                    })
                            }
                            $http.get('/users/facebookAccessToken/').
                                success(function (data) {
                                    console.log(data)
                                    getFbPages('https://graph.facebook.com/v2.3/me?fields=likes&access_token=CAACEdEose0cBAABPUdlSmjwteEoQYLwNoFvGOAOGpozBkuRbERMDZB92FTuMTeTvNTLX9ZBGPRAVIvdssK8ntDcAe3cXqKsZBfQZC1dQmEZABIwKnhoT8uiNJZCCU6WrjZC3VLRFoX4KJXEwwo0tLa4Sqa5ajpVxLhFRkRZBS5KYf2NFbnHHgk0JkcZAqNk9knPCwSohvvO6iM25kjljedltPiDgGXAaNp3sZD')

                                    $http.get('https://graph.facebook.com/v2.3/me/accounts?access_token=CAACEdEose0cBAABPUdlSmjwteEoQYLwNoFvGOAOGpozBkuRbERMDZB92FTuMTeTvNTLX9ZBGPRAVIvdssK8ntDcAe3cXqKsZBfQZC1dQmEZABIwKnhoT8uiNJZCCU6WrjZC3VLRFoX4KJXEwwo0tLa4Sqa5ajpVxLhFRkRZBS5KYf2NFbnHHgk0JkcZAqNk9knPCwSohvvO6iM25kjljedltPiDgGXAaNp3sZD').
                                        success(function (data) {
                                            console.log(data)
                                        })

                                }).
                                error(function (data) {
                                })
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