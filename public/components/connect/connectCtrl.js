angular.module('claudeApp').controller('connectCtrl', ['$scope', '$rootScope', '$http',
    'ArtistsFactory', 'UserFactory', 'OrganizerFactory', 'EventsFactory', 'PlaceFactory',
    'StoreRequest', '$timeout', 'InfoModal', '$location',
    function ($scope, $rootScope, $http, ArtistsFactory, UserFactory, OrganizerFactory,
              EventsFactory, PlaceFactory, StoreRequest, $timeout, InfoModal, $location) {

        var token;

        function followArtist (id, toCreate) {
            ArtistsFactory.followArtistByFacebookId(id).then(function (isFollowed) {
            }, function (error) {
                if (error !== 409 && toCreate == true) {
                    getArtistPage(id);
                }
            })
        }

        function followOrganizer (id, toCreate) {
            OrganizerFactory.followOrganizerByFacebookId(id).then(function (isFollowed) {
            }, function (error) {
                if (error !== 409 && toCreate == true) {
                    getOrganizerPage(id);
                }
            });
        }

        function followPlace (id, toCreate) {
            PlaceFactory.followPlaceByFacebookId(id).then(function (isFollowed) {
            }, function (error) {
                if (error !== 409 && toCreate == true) {
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

        function postEventId (event) {
            EventsFactory.postEventToCreate(event.id)
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
                        getOrganizerEvents(organizer.id);
                        followOrganizer(organizer.id, false)
                    }
                });
            }
        }

        function getOrganizerEvents (id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?fields=events&access_token=' + token).
                success(function (events) {
                    if (events.events != undefined) {
                        events.events.data.forEach(postEventId)
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
            $http.get('https://graph.facebook.com/v2.2/' + place.id + '/?fields=cover, picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
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
                            getOrganizerEvents(newPlace.facebookId);
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
            if (place.description == undefined) {
                place.description;
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
                        getOrganizerEvents(newPlace.facebookId);
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
                    if (flag == 0) {
                        var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;;
                        if (data.description != undefined) {
                            data.description = '<div class="column large-12">' + data.description + '</div>';
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

        function getLikes (data) {
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
                if (pages[i].category.toLowerCase() == 'concert tour') {
                    //follow organizer
                    //post if not exist
                    followOrganizer(pages[i].id, true);
                } else if (pages[i].category.toLowerCase() == "musician/band") {
                    //followArtist
                    // post if not exist
                    followArtist(pages[i].id, true);
                } else if (pages[i].category.toLowerCase() == "concert venue" ||
                    pages[i].category.toLowerCase() == 'club' ||
                    pages[i].category.toLowerCase() == 'bar' ||
                    pages[i].category.toLowerCase() == 'arts/entertainment/nightlife') {
                    //followPlace
                    // post if not exist
                    followPlace(pages[i].id, true);
                } else if (pages[i].category_list != undefined) {
                    for (var ii = 0; ii < pages[i].category_list.length; ii++) {
                        if (pages[i].category_list[ii].name.toLowerCase() == 'concert venue' ||
                            pages[i].category_list[ii].name.toLowerCase() == 'club' ||
                            pages[i].category_list[ii].name.toLowerCase() == 'bar' ||
                            pages[i].category_list[ii].name.toLowerCase() == "nightlife") {
                            followPlace(pages[i].id, true);
                        }
                    }
                }
            }
            if (next != undefined) {
                $http.get(next).
                    success(function (data) {
                        getLikes(data)
                    })
            }
        }

        function getMusicPages (data) {
            var pages;
            var next;
            var pagesLength;
            if (data.music != undefined) {
                pages = data.music.data;
                if (data.music.paging.next != undefined) {
                    next = data.music.paging.next
                }
                pagesLength = data.music.data.length;
            } else {
                pages = data.data;
                if (data.paging != undefined) {
                    next = data.paging.next;
                }
                pagesLength = data.data.length;
            }
            for (var i = 0; i < pagesLength; i++) {
                if (pages[i].category.toLowerCase() == 'concert tour') {
                    //follow organizer
                    //post if not exist
                    followOrganizer(pages[i].id, true);
                } else if (pages[i].category.toLowerCase() == "musician/band") {
                    //followArtist
                    // post if not exist
                    followArtist(pages[i].id, true);
                } else if (pages[i].category.toLowerCase() == "concert venue" ||
                    pages[i].category.toLowerCase() == 'club' ||
                    pages[i].category.toLowerCase() == 'bar' ||
                    pages[i].category.toLowerCase() == 'arts/entertainment/nightlife') {
                    //followPlace
                    // post if not exist
                    followPlace(pages[i].id, true);
                } else if (pages[i].category_list != undefined) {
                    for (var ii = 0; ii < pages[i].category_list.length; ii++) {
                        if (pages[i].category_list[ii].name.toLowerCase() == 'concert venue' ||
                            pages[i].category_list[ii].name.toLowerCase() == 'club' ||
                            pages[i].category_list[ii].name.toLowerCase() == 'bar' ||
                            pages[i].category_list[ii].name.toLowerCase() == "nightlife") {
                            followPlace(pages[i].id, true);
                        }
                    }
                }
            }
            if (next != undefined) {
                $http.get(next).
                    success(function (data) {
                        getMusicPages(data)
                    })
            }
        }

        function getEvents (data) {
            var events;
            var next;
            var eventsLength;
            if (data.events != undefined) {
                events = data.events.data;
                if (data.events.paging.next != undefined) {
                    next = data.events.paging.next
                }
                eventsLength = data.events.data.length;
            } else {
                events = data.data;
                if (data.paging != undefined) {
                    next = data.paging.next;
                }
                eventsLength = data.data.length;
            }
            for (var i = 0; i < eventsLength; i++) {
                if (events[i].owner !== undefined) {
                    if (events[i].owner.id) {
                        followOrganizer(events[i].owner.id, true);
                    }
                }
                if (events[i].place !== undefined) {
                    if (events[i].place.id) {
                        followPlace(events[i].place.id, true);
                    }
                }
                postEventId(events[i]);
            }
            if (next != undefined) {
                $http.get(next).
                    success(function (data) {
                        getEvents(data)
                    })
            }
        }

        function getFbPages (route) {
            $http.get(route).
                success(function (data) {
                    getLikes(data);
                    getMusicPages(data);
                    getEvents(data)
                })
        }

        function getUserToken() {
            UserFactory.getToken().then(function (newToken) {
                token = newToken;
                getFbPages('https://graph.facebook.com/v2.3/me?fields=id,name,music,events{place,owner,admins},likes&access_token=' + token);
            });
        }

        function applyLastRequest() {
            if ($rootScope.lastReq.method == 'post') {
                if ($rootScope.lastReq.object != "") {
                    $http.post($rootScope.lastReq.path, $rootScope.lastReq.object).
                        success(function (data) {
                            InfoModal.displayInfo($rootScope.lastReq.success);
                            $rootScope.lastReq = {};
                        }).
                        error(function (data) {
                            if (data.error == 'Credentials required') {
                                StoreRequest.storeRequest('post',
                                    $rootScope.lastReq.path, $rootScope.lastReq.object,
                                    $rootScope.lastReq.success)
                            } else {
                                InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
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
                            }
                        })
                }
            }
        }

        function getConnected (connectWin) {
            var waitForConnected = setInterval(function () {
                if (connectWin.document.getElementById('top') != undefined &&
                    connectWin.document.getElementById('top') != null) {
                        clearInterval(waitForConnected);
                    if (connectWin.document.getElementById('top').getAttribute("ng-init") ==
                        '$root.connected = true') {
                        $timeout(function () {
                            $rootScope.$apply(function () {
                                $rootScope.connected = true;
                                connectWin.close();
                                InfoModal.displayInfo('Vous êtes connécté')
                            })
                        }, 0);
                        UserFactory.makeFavoriteTracksRootScope();
                        getUserToken();
                        if ($rootScope.lastReq != {} && $rootScope.lastReq != undefined) {
                            applyLastRequest();
                        }
                    }
                }
            }, 500);
        }

        $scope.connectLink = function (url) {
            var connectWin = window.open($location.host() + url, "", "toolbar=no, scrollbars=no, resizable=no, width=500, height=500");
            var changePath = setInterval(function() {
                console.log(connectWin.location);
                if (connectWin.location == undefined && connectWin.location != {}) {
                    clearInterval(changePath);
                    InfoModal.displayInfo('Une erreure c\'est produite', 'error')
                }
                if (connectWin.location.href.indexOf('#/connected') > -1) {
                    clearInterval(changePath);
                    getConnected(connectWin);
                }
            }, 1000);
        };
    }]);