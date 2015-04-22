app.controller('connectCtrl', function ($scope, $rootScope, $http, $modal, ArtistsFactory) {
    $scope.connectByMail = function () {
        $http.post('/authenticate/userpass', {username: $scope.username, password: $scope.password}).
            success(function (data) {

            }).
            error(function (data) {

            })
    };
    function followArtist (id, token) {
        $http.post('/artists/' + id +'/followByFacebookId').
            success(function () {
                console.log(data)
            }).error(function (data) {
                if (token != false) {
                    getArtistPage(id, token)
                }
            })
    }
    function postArtist (artist) {
        ArtistsFactory.postArtist(artist.name, artist).then(function (tracks) {
            $http.post('/artists/' + artist.id +'/followByFacebookId').
                success(function (data) {
                    console.log(data);
                    followArtist(artist.id, false)
                })
        });
    }
    function getArtistPage (id, token) {
        $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
            success(function (data) {
                console.log(data);
                postArtist(data)
            })
    }
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
                            function getFbPages (route, token) {
                                $http.get(route + token).
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
                                                //post if not exist
                                            } else if (pages[i].category == "Musician/band") {
                                                //followArtist
                                                var pageId = pages[i].id;
                                                followArtist(pageId, token);
                                                // post if not exist
                                            } else if (pages[i].category == "concert venue") {
                                                //folowPlace
                                                // post if not exist
                                            }
                                        }
                                        getFbPages(next)
                                    })
                            }
                            $http.get('/users/facebookAccessToken/').
                                success(function (data) {
                                    console.log(data)
                                    var token = 'CAACEdEose0cBAB2QWdAlF1PeOxPCHGXrg7X1SYrQaqOaClyZB6DVH9mYBVYSyBFjmq1Xf3paRIO0wmCsBPzP56lwSoQGFwm9BSmY8lI3PREoz2kmuBchT0h8RKXNg6x3qEsfZC5wwtWXDYWuq72e5JDJP9PRqCcnkY5nJvu3E3ZA9IXgaiGiBgNAuwylZC2cBgxf3c3gUPmLMkRUYzs6DKiMc5fflZBMZD'
                                    getFbPages('https://graph.facebook.com/v2.3/me?fields=likes&access_token=', token);

                                    /*$http.get('https://graph.facebook.com/v2.3/me/accounts?access_token=CAACEdEose0cBAABPUdlSmjwteEoQYLwNoFvGOAOGpozBkuRbERMDZB92FTuMTeTvNTLX9ZBGPRAVIvdssK8ntDcAe3cXqKsZBfQZC1dQmEZABIwKnhoT8uiNJZCCU6WrjZC3VLRFoX4KJXEwwo0tLa4Sqa5ajpVxLhFRkRZBS5KYf2NFbnHHgk0JkcZAqNk9knPCwSohvvO6iM25kjljedltPiDgGXAaNp3sZD').
                                        success(function (data) {
                                            console.log(data)
                                        })*/

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