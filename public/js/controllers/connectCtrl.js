app.controller('connectCtrl',
    function ($scope, $rootScope, $http, $modal, ArtistsFactory, UserFactory, OrganizerFactory, EventsFactory) {

    var token = '';

    $scope.connectByMail = function () {
        $http.post('/authenticate/userpass', {username: $scope.username, password: $scope.password}).
            success(function (data) {

            }).
            error(function (data) {

            })
    };

    function followArtist (id, toCreate) {
        ArtistsFactory.followArtistByFacebookId(id).then(function (isFollowed) {
            if (isFollowed == 'error' && toCreate == true) {
                getArtistPage(id);
            }
        })
    }

    function followOrganizer (id, toCreate) {
        OrganizerFactory.followOrganizerByFacebookId(id).then(function (isFollowed) {
            if (isFollowed == 'error' && toCreate == true) {
                getOrganizerPage(id);
                getOrganizerEvents(id);
            }
        });
    }
    function postArtist (artist) {
        if (artist.cover != undefined) {
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
            ArtistsFactory.postArtist(artist.name, artist).then(function (isCreated) {
                if (isCreated != 'error') {
                    followArtist(artist.id, false)
                } else {
                }
            });
        }
    }

    function getArtistPage (id) {
        $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
            success(function (data) {
                postArtist(data)
            })
    }

    function postOrganizer (organizer) {
        if (organizer.cover != undefined) {
            var newOrganizer = {};
            newOrganizer.facebookId = organizer.id;
            newOrganizer.name = organizer.name;
            newOrganizer.description = organizer.about;
            newOrganizer.websites = organizer.website;
            newOrganizer.imagePath = organizer.cover.source;
            OrganizerFactory.createOrganizer(newOrganizer).then(function (isCreated) {
                if (isCreated != 'error') {
                    followOrganizer(organizer.id, false)
                }
            });
        }
    }
    function postEvent (event) {
        if (event.end_time != 'Invalid Date') {
            event.endTime = new Date(event.end_time)
            event.endTime = $filter('date')(event.endTime, "yyyy-MM-dd HH:mm")
        }
        event.startTime = new Date(event.start_time);
        event.adresses = [{
            cities: event.venue.city,
            geographicPoints: event.venue.latitude + ', ' + event.venue.longitude,
            streets: event.venue.street,
            zips: event.venue.zip
        }];
        var newEvent = {
            name: event.name,
            description: event.description,
            startTime: $filter('date')(event.startTime, "yyyy-MM-dd HH:mm"),
            endTime: event.endTime,
            images: event.cover.source,
            places: event.location,
            facebookId: event.id,
            isPublic: true,
            addresses: event.adresses
        }
        EventsFactory.postEvent(newEvent).then(function (eventPosted) {
            console.log(eventPosted)
        })
    }
    function getEventInfosAndPostIt (event) {
        $http.get('https://graph.facebook.com/v2.3/' + event.id + '?fields=cover,description,name,start_time,end_time,owner,venue&access_token=' + token).
            success(function (event) {
                console.log(event)
                postEvent(event)
            })
    }

    function getOrganizerEvents (id) {
        $http.get('https://graph.facebook.com/v2.3/' + id + '?fields=events&access_token=' + token).
            success(function (events) {
                if (events.events != undefined) {
                    console.log(events)
                    events.events.data.forEach(getEventInfosAndPostIt)
                }
            })
    }
    function getOrganizerPage (id) {
        $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
            success(function (data) {
                postOrganizer(data)
            })
    }

    function getFbPages (route) {
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
                    if (data.paging != undefined) {
                        next = data.paging.next;
                    }
                    pagesLength = data.data.length;
                }
                for (var i = 0; i < pagesLength; i++) {
                    if (pages[i].category == 'Concert tour') {
                        //follow organizer
                        //post if not exist
                        console.log(pages[i])
                        followOrganizer(pages[i].id, true);
                    } else if (pages[i].category == "Musician/band") {
                        //followArtist
                        //followArtist(pages[i].id, true);
                        // post if not exist
                    } else if (pages[i].category == "concert venue") {
                        //followPlace
                        // post if not exist
                    }
                }
                if (next != undefined) {
                    getFbPages(next)
                }
            })
    }

    function getUserToken() {
        UserFactory.getToken().then(function (newToken) {
            token = newToken;
            getFbPages('https://graph.facebook.com/v2.3/me?fields=likes&access_token=' + token);
        });
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
        if (connectWin.document.getElementById('top') != undefined || connectWin.document.getElementById('top') != null) {
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