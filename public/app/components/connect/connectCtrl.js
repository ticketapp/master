angular.module('claudeApp').controller('connectCtrl', ['$scope', '$rootScope', '$http',
    'ArtistsFactory', 'UserFactory', 'OrganizerFactory', 'EventsFactory', 'PlaceFactory',
    'StoreRequest',
    function ($scope, $rootScope, $http, ArtistsFactory, UserFactory, OrganizerFactory,
              EventsFactory, PlaceFactory, StoreRequest) {

        var token = '';

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
                    //getOrganizerPage(id);
                    //getOrganizerEvents(id);
                }
            });
        }

        function followPlace (id, toCreate) {
            PlaceFactory.followPlaceByFacebookId(id).then(function (isFollowed) {
                if (isFollowed == 'error' && toCreate == true) {
                    getPlacePage(id);
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

        function getOrganizerEvents (id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?fields=events&access_token=' + token).
                success(function (events) {
                    if (events.events != undefined) {
                        //events.events.data.forEach(postEventId)
                    }
                })
        }
        function getOrganizerPage (id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
                success(function (data) {
                    postOrganizer(data)
                })
        }


        function getCoverPlace(place) {
            $http.get('https://graph.facebook.com/v2.2/' + searchPlaces.id + '/?fields=cover, picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function (data) {
                    var newPlace = {
                        name: place.name,
                        facebookId: place.id,
                        capacity: place.checkins,
                        description: place.description,
                        webSite: place.website,
                        imagePath: data.source,
                        address: {
                            city: place.location.city,
                            zip: place.location.zip,
                            street: place.location.street
                        }
                    };
                    PlaceFactory.postPlace(newPlace).then(function (isCreated) {
                        if (isCreated != 'error') {
                            followPlace(newPlace.facebookId, false)
                        }
                    })
                }).
                error(function () {
                    var newPlace = {
                        name: place.name,
                        facebookId: place.id,
                        capacity: place.checkins,
                        description: place.description,
                        webSite: place.website,
                        address: {
                            city: place.location.city,
                            zip: place.location.zip,
                            street: place.location.street
                        }
                    };
                    PlaceFactory.postPlace(newPlace).then(function (isCreated) {
                        if (isCreated != 'error') {
                            followPlace(newPlace.facebookId, false)
                        }
                    })
                })
        }

        function getPositionAndCreate (place) {
            if (place.location.street == undefined) {
                place.location.street = '';
            }
            if (place.location.zip == undefined) {
                place.location.zip = '';
            }
            if (place.location.city == undefined) {
                place.location.city = '';
            }
            if (place.cover != undefined) {
                var newPlace = {
                    name: place.name,
                    facebookId: place.id,
                    capacity: place.checkins,
                    description: place.description,
                    webSite: place.website,
                    imagePath : place.cover.source,
                    address : {
                        city: place.location.city,
                        zip: place.location.zip,
                        street: place.location.street
                    }
                };
                PlaceFactory.postPlace(newPlace).then(function (isCreated) {
                    if (isCreated != 'error') {
                        followPlace(newPlace.facebookId, false)
                    }
                })
            } else {
                getCoverPlace(place);
            }
        }

        function getInfoPlace (place) {
            $http.get('https://graph.facebook.com/v2.2/'+ place.id +'/?fields=checkins,cover,description,' +
                'hours,id,likes,link,location,name,phone,website,picture&access_token=' + token).
                success(function(data, status, headers, config) {
                    var flag = 0;
                    if (data.location != undefined) {
                        if (data.location.country == undefined || data.location.country != 'France') {
                            flag = 1;
                        }
                    } else {
                        flag = 1;
                    }
                    console.log(data)
                    if (flag == 0) {
                        var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;;
                        if (data.description == undefined) {
                            data.description = "";
                        }
                        data.description = data.description.replace(/(\n\n)/g, " <br/><br/></div><div class='column large-12'>");
                        data.description = data.description.replace(/(\n)/g, " <br/>");
                        if (matchedLinks = data.description.match(links)) {
                            var m = matchedLinks;
                            var unique = [];
                            for (var ii = 0; ii < m.length; ii++) {
                                var current = m[ii];
                                if (unique.indexOf(current) < 0) unique.push(current);
                            }
                            for (var i=0; i < unique.length; i++) {
                                data.description = data.description.replace(new RegExp(unique[i],"g"),
                                        "<a href='" + unique[i]+ "'>" + unique[i] + "</a>")
                            }
                        }
                        getPositionAndCreate(data);
                    }
                }).
                error(function(data, status, headers, config) {

                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }

        function getPlacePage (id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
                success(function (data) {
                    getInfoPlace(data)
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
                            //followOrganizer(pages[i].id, true);
                        } else if (pages[i].category == "Musician/band") {
                            //followArtist
                            // post if not exist
                            followArtist(pages[i].id, true);
                        } else if (pages[i].category == "concert venue" ||
                            pages[i].category == 'Club' ||
                            pages[i].category == 'Bar' ||
                            pages[i].category == 'Arts/entertainment/nightlife') {
                            //followPlace
                            // post if not exist
                            followPlace(pages[i].id, true);
                        } else if (pages[i].category_list != undefined) {
                            for (var ii = 0; ii < pages[i].category_list.length; ii++) {
                                if (pages[i].category_list[ii].name == 'Concert Venue' ||
                                    pages[i].category_list[ii].name == 'Club' ||
                                    pages[i].category_list[ii].name == 'Bar' ||
                                    pages[i].category_list[ii].name == "Nightlife") {
                                    followPlace(pages[i].id, true);
                                }
                            }
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
                                StoreRequest.storeRequest('post', $rootScope.lastReq.path, $rootScope.lastReq.object, $rootScope.lastReq.success)
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
                                StoreRequest.storeRequest('post', $rootScope.lastReq.path, $rootScope.lastReq.object, $rootScope.lastReq.success)
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
    }]);