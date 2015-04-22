app.controller('connectCtrl', function ($scope, $rootScope, $http, $modal, ArtistsFactory) {
    $scope.connectByMail = function () {
        $http.post('/authenticate/userpass', {username: $scope.username, password: $scope.password}).
            success(function (data) {

            }).
            error(function (data) {

            })
    };
    var followErrorCount = 0;
    var nbAskToFollow = 0;
    function followArtist (id, token) {
        nbAskToFollow++;
        console.log('nbAskToFollow:' + nbAskToFollow);
        ArtistsFactory.followArtistByFacebookId(id).then(function (isFollowed) {
            if (isFollowed == 'error') {
                followErrorCount++;
                console.log('followErrors:' + followErrorCount);
                getArtistPage(id, token);
            }

        });
    }
    var nbArtPost = 0;
    var nbErrorPost = 0;
    function postArtist (artist) {
        if (artist.cover != undefined) {
            nbArtPost++;
            console.log('nbArtPost:' + nbArtPost);
            artist.facebookUrl = artist.link.replace('https://www.facebook.com/', '').replace('pages').replace(/[/]/g, '');
            artist.facebookId = artist.id;
            artist.imagePath = artist.cover.source;
            if (artist.website != undefined) {
                artist.websites = artist.website;
            } else {
                artist.websites ='';
            }
            if (artist.bio != undefined) {
                artist.description = artist.bio;
            } else {
                artist.description ='';
            }
            if (artist.genre == undefined) {
                artist.genre = "";
            }
            artist.name = artist.username;
            ArtistsFactory.postArtist(artist.name, artist).then(function (isCreated) {
                console.log(isCreated);
                if (isCreated != 'error') {
                    followArtist(artist.id, false)
                } else {
                    nbErrorPost++;
                    console.log('nbErrorPost:' + nbErrorPost)
                }
            });
        }
    }

    var nbFbPagesGet = 0;
    function getArtistPage (id, token) {
        $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
            success(function (data) {
                console.log(data);
                nbFbPagesGet++;
                console.log('nbFbPagesGet:' + nbFbPagesGet);
                postArtist(data)
            })
    }
    var nbArtFbPagesFollowedOnFb = 0;
    function getFbPages (route, token) {
        $http.get(route).
            success(function (data) {
                var pages;
                var next;
                var pagesLength;
                if (data.likes != undefined) {
                    pages = data.likes.data;
                    if (data.likes.paging.next != undefined) {
                        next = data.likes.paging.next
                    }
                    pagesLength = data.likes.data.length;
                } else {
                    pages = data.data;
                    if (data.paging.next != undefined) {
                        next = data.paging.next;
                    }
                    pagesLength = data.data.length;
                }
                for (var i = 0; i < pagesLength; i++) {
                    if (pages[i].category == 'Concert tour') {
                        //folow organizer
                        //post if not exist
                    } else if (pages[i].category == "Musician/band") {
                        //followArtist
                        nbArtFbPagesFollowedOnFb++;
                        console.log('nbArtFbPagesFollowedOnFb:' + nbArtFbPagesFollowedOnFb)
                        followArtist(pages[i].id, token);
                        // post if not exist
                    } else if (pages[i].category == "concert venue") {
                        //folowPlace
                        // post if not exist
                    }
                }
                if (next == undefined) {
                    getFbPages(next, token)
                }
            })
    }

    function getUserToken() {
        $http.get('/users/facebookAccessToken/').
            success(function (data) {
                var token = 'CAACEdEose0cBAIyQ2vi7viKASjMb8KoK8PuKs3OdpVWxvVW4oaZAHmkWlsBBkEZBGgclZCXVhh5PfNMxDtIOcjONV0RyNXnRSQPV4hG70VoNeBortntC8wKbTio5bF3aazZC8ummsHPKDGjO5872F3qc2ph9mFg8kWedyj92ZCIwAzpZAP05ZBZAwqZBe458ZC6DpjYHMVIycwcireCZAETsMevOdrJLc9N5moZD'
                getFbPages('https://graph.facebook.com/v2.3/me?fields=likes&access_token=' + token, token);
            }).
            error(function (data) {
            })
    }

    function applyLastRequest() {
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

    function getConnected (connectWin) {
        if (connectWin.document.getElementById('top') != undefined || connectWin.document.getElementById('top') != 'null') {
            if (connectWin.document.getElementById('top').getAttribute("ng-init") == '$root.connected = true') {
                $rootScope.passConnectedToTrue();
                connectWin.close();
                getUserToken();
                if ($rootScope.lastReq != {}) {
                    applyLastRequest();
                }
            }
        } else {
            getConnected();
        }
    }
    $scope.connectLink = function (url) {
        var connectWin = window.open(url, "", "toolbar=no, scrollbars=no, resizable=no, width=500, height=500");
        var changePath = setInterval(function() {
            if (connectWin.location.href == undefined) {
                clearInterval(changePath)
            }
            if (connectWin.location.href == 'http://localhost:9000/#/connected') {
                clearInterval(changePath);
                getConnected(connectWin);
            }
        }, 1000);
    };
});