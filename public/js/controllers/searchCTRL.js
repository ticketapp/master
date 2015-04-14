app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$filter', 'oboe', '$timeout', function($rootScope, $http, $scope, $filter, oboe, $timeout){
    $scope.limit = 20;
    $scope.artists = [];
    $scope.artistsFb = [];
    $scope.users = [];
    $scope.places = [];
    $scope.events = [];
    $scope.loadingMore = true;
    var offset = 0;
    var _selArtist = $rootScope.activArtist;
    var _selEvent = $rootScope.activEvent;
    var _selUsr = $rootScope.activUsr;
    var _selPlace = $rootScope.activPlace;
    var _selStart = ($rootScope.maxStart-23)*24;
    if (document.getElementById('searchBar') != null) {
        var _research = document.getElementById('searchBar').value.trim();
    } else {
        var _research = '';
    }
    $scope.createNewPlace = function (place) {
        $http.post('/places/create', {
            name: place.name,
            facebookId: place.facebookId,
            geographicPoint: place.geographicPoint,
            capacity: place.capacity,
            description: place.description,
            webSite: place.webSite,
            imagePath : place.imagePath
        }).success(function(data){
            console.log(data)
            window.location.href =('#/lieu/' + data.placeId);
        }).error(function(data){
            console.log(data)
        })
    }
    $scope.searchNewPlace = function (placeName) {
        function getPlacesById (searchPlaces) {
            $http.get('https://graph.facebook.com/v2.2/'+ searchPlaces.id +'/?fields=checkins,cover,description,hours,id,likes,link,location,name,phone,website,picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function(data, status, headers, config) {
                    flag = 0;
                    if (data.location != undefined) {
                        if (data.location.country == undefined || data.location.country != 'France') {
                            flag = 1;
                        }
                    } else {
                        flag = 1;
                    }
                    if (flag == 0){
                        //count2 = count2 + 1;
                        //console.log("c2", count2);
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
                        function getPositionAndCreate (place) {
                            $http.get('https://maps.googleapis.com/maps/api/geocode/json?address=' +
                                place.location.street + '+' +
                                place.location.zip + '+' +
                                place.location.city + '+' +
                                place.location.country + '&key=AIzaSyDx-k7jA4V-71I90xHOXiILW3HHL0tkBYc').
                                success(function (data) {
                                    console.log(place)
                                    var loc = '(' + data.results[0].geometry.location.lat +
                                        ',' + data.results[0].geometry.location.lng + ')';
                                    if (place.cover != undefined) {
                                        $scope.places.push({
                                            placeId: -1,
                                            name: place.name,
                                            facebookId: place.id,
                                            geographicPoint: loc,
                                            capacity: place.checkins,
                                            description: place.description,
                                            webSite: place.website,
                                            imagePath: place.cover.source
                                        });
                                        $rootScope.resizeImgHeight();
                                    } else {
                                        $http.get('https://graph.facebook.com/v2.2/'+ searchPlaces.id +'/?fields=cover, picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                                            success(function (data) {
                                                console.log(data)
                                                $scope.places.push({
                                                    placeId: -1,
                                                    name: place.name,
                                                    facebookId: place.id,
                                                    geographicPoint: loc,
                                                    capacity: place.checkins,
                                                    description: place.description,
                                                    webSite: place.website,
                                                    imagePath: data.source
                                                })
                                                $rootScope.resizeImgHeight();
                                            }).
                                            error(function () {
                                                $scope.places.push({
                                                    placeId: -1,
                                                    name: place.name,
                                                    facebookId: place.id,
                                                    geographicPoint: loc,
                                                    capacity: place.checkins,
                                                    description: place.description,
                                                    webSite: place.website
                                                })
                                                $rootScope.resizeImgHeight();
                                            })
                                    }
                                });
                        }
                        //places.push(data);
                        getPositionAndCreate(data);
                    }
                }).
                error(function(data, status, headers, config) {
                    console.log(data);
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
        $http.get('https://graph.facebook.com/v2.2/search?q=' + placeName + '&type=page&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
            success(function (data, status, headers, config) {
                data = data.data;
                console.log(data)
                for (var iv = 0; iv < data.length; iv++) {
                    if (data[iv].category == 'Concert venue' ||
                        data[iv].category == 'Club' ||
                        data[iv].category == 'Bar' ||
                        data[iv].category == 'Arts/entertainment/nightlife') {
                        getPlacesById(data[iv])
                        console.log(data[iv])
                    } else if (data[iv].category_list != undefined) {
                        for (var ii = 0; ii < data[iv].category_list.length; ii++) {
                            if (data[iv].category_list[ii].name == 'Concert Venue' ||
                                data[iv].category_list[ii].name == 'Club' ||
                                data[iv].category_list[ii].name == 'Bar' ||
                                data[iv].category_list[ii].name == "Nightlife") {
                                    getPlacesById(data[iv]);
                            }
                        }
                    }
                }
            }).
            error(function (data, status, headers, config) {
                console.log(data)
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
    function search () {
        $rootScope.activArtist = _selArtist;
        $rootScope.activEvent = _selEvent;
        $rootScope.activPlace = _selPlace;
        $rootScope.activUsr = _selUsr;
        $scope.filterSearch = _research;
        if (_research.length == 0) {
            if (_selEvent == true) {
                var eventsLenght = $scope.events.length;
                var maxStartTime =  _selStart*3600000 + new Date().getTime();
                for (var e = 0; e < eventsLenght; e++) {
                    console.log(maxStartTime)
                    if ($scope.events[e].startTime > maxStartTime) {
                        $scope.events.splice(e, 1)
                        $scope.$apply();
                        e = e -1;
                        eventsLenght = eventsLenght - 1;
                    }
                }
                $http.get('/events/offsetAndMaxStartTime/'+ offset+ '/' + $rootScope.geoLoc + '/' + _selStart).
                    success(function (data, status, headers, config) {
                        var scopeIdList = [];
                        function getEventId(el, index, array) {
                            scopeIdList.push(el.eventId);
                        }
                        $scope.events.forEach(getEventId);
                        function uploadEvents(el, index, array) {
                            if (scopeIdList.indexOf(el.eventId) == -1) {
                                el.priceColor = 'rgb(0, 140, 186)';
                                if (el.tariffRange != undefined) {
                                    var tariffs = el.tariffRange.split('-');
                                    if (tariffs[1] > tariffs[0]) {
                                        el.tariffRange = tariffs[0].replace('.0', '') + '€ - ' +
                                            tariffs[1].replace('.0', '') + '€';
                                    } else {
                                        el.tariffRange = tariffs[0].replace('.0', '') + '€';
                                    }
                                    el.priceColor = 'rgb(' + tariffs[0]*10 + ',' + (250 - (tariffs[0]*10 ) )+
                                        ',' + (175 - (tariffs[0]*10 )) + ')'
                                }
                                $scope.events.push(el);
                            }
                        }
                        data.forEach(uploadEvents)
                        console.log($scope.events)
                        $rootScope.resizeImgHeight();
                        $scope.loadingMore = false;
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selArtist == true) {
                $http.get('/artists/since/' + offset + '/20').
                    success(function (data, status, headers, config) {
                        var scopeIdList = [];
                        function getArtistId(el, index, array) {
                            scopeIdList.push(el.artistId);
                        }
                        $scope.artists.forEach(getArtistId);
                        function uploadArtists(el, index, array) {
                            if (scopeIdList.indexOf(el.artistId) == -1) {
                                $scope.artists.push(el);
                            }
                        }
                        data.forEach(uploadArtists)
                        $rootScope.resizeImgHeight();
                        $scope.loadingMore = false;
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selUsr == true) {
                $http.get('/users').
                    success(function(data, status, headers, config) {
                        if (data != $scope.users) {
                            $scope.users = data;
                        }
                        $rootScope.resizeImgHeight()
                        $scope.loadingMore = false;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selPlace == true) {
                $http.get('/places/offset/'+ offset+ '/' + $rootScope.geoLoc).
                    success(function(data, status, headers, config) {
                        var scopeIdList = [];
                        function getPlaceId(el, index, array) {
                            scopeIdList.push(el.placeId);
                        }
                        $scope.places.forEach(getPlaceId);
                        function uploadPlaces(el, index, array) {
                            if (scopeIdList.indexOf(el.placeId) == -1) {
                                if (el.geographicPoint != undefined) {
                                    el.geographicPoint = el.geographicPoint.replace("(", "");
                                    el.geographicPoint = el.geographicPoint.replace(")", "");
                                    el.geographicPoint = el.geographicPoint.replace(",", ", ");
                                }
                                $scope.places.push(el);
                            }
                        }
                        data.forEach(uploadPlaces)
                        console.log($scope.places)
                        $rootScope.resizeImgHeight();
                        $scope.loadingMore = false;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            $scope.artistsFb = [];
        } else {
            if (_selArtist == true) {
                $scope.artistsFb = $filter('filter')($scope.artistsFb, {name :  _research});
                $http.get('/artists/containing/'+_research).
                    success(function(data, status, headers, config) {
                        var scopeIdList = [];
                        function getArtistId(el, index, array) {
                            scopeIdList.push(el.artistId);
                        }
                        $scope.artists.forEach(getArtistId);
                        if ($scope.artists.length == 0) {
                            $scope.artists = data;
                        } else {
                            function uploadArtists(el, index, array) {
                                if (scopeIdList.indexOf(el.artistId) == -1) {
                                    $scope.artists.push(el);
                                }
                            }
                            data.forEach(uploadArtists)
                        }
                        $rootScope.resizeImgHeight();
                        $scope.loadingMore = false;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selPlace == true) {
                $scope.places = $filter('filter')($scope.places, {name :  _research});
                $http.get('/places/containing/'+_research).
                    success(function(data, status, headers, config) {
                        var scopeIdList = [];
                        function getPlaceId(el, index, array) {
                            scopeIdList.push(el.placeId);
                        }
                        $scope.places.forEach(getPlaceId);
                            function uploadPlaces(el, index, array) {
                                if (scopeIdList.indexOf(el.placeId) == -1) {
                                    if (el.geographicPoint != undefined) {
                                        el.geographicPoint = el.geographicPoint.replace("(", "");
                                        el.geographicPoint = el.geographicPoint.replace(")", "");
                                        el.geographicPoint = el.geographicPoint.replace(",", ", ");
                                    }
                                    $scope.places.push(el);
                                }
                            }
                            data.forEach(uploadPlaces)
                        $rootScope.resizeImgHeight()
                        $scope.loadingMore = false;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selUsr == true) {
                $scope.users.forEach(getUsersId);
                $scope.users = $filter('filter')($scope.users, {nickname :  _research});
                $http.get('/users/containing/'+_research).
                    success(function(data, status, headers, config) {
                        var scopeIdList = [];
                        function getUsersId(el, index, array) {
                            scopeIdList.push(el.userId);
                        }
                        if ($scope.users.length == 0) {
                            $scope.users = data;
                        } else {
                            function uploadUsers(el, index, array) {
                                if ($scope.users.indexOf(el) < -1) {
                                    $scope.users.push(el);
                                }
                            }
                            data.forEach(uploadUsers);
                        }
                        $rootScope.resizeImgHeight()
                        $scope.loadingMore = false;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selEvent == true) {
                $timeout(function() {
                    $scope.$apply(function () {
                        $scope.events = $filter('filter')($scope.events, {name: _research})
                        $scope.scopeIdList = [];
                    })
                },0);
                $http.get('/events/containing/' + _research + '/' + $rootScope.geoLoc).
                    success(function (data, status, headers, config) {
                        function uploadEvents(el, index, array) {
                            $timeout(function() {
                                $scope.$apply(function () {
                                    function getEventsId(el, index, array) {
                                        $scope.scopeIdList.push(el.eventId);
                                    }
                                    $scope.events.forEach(getEventsId);
                                });
                                if ($scope.scopeIdList.indexOf(el.eventId) == -1) {
                                    if (el.tariffRange != undefined) {
                                        var tariffs = el.tariffRange.split('-');
                                        if (tariffs[1] > tariffs[0]) {
                                            el.tariffRange = tariffs[0].replace('.0', '') +
                                                '€ - ' + tariffs[1].replace('.0', '') + '€';
                                        } else {
                                            el.tariffRange = tariffs[0].replace('.0', '') + '€';
                                        }
                                    }
                                    $scope.events.push(el);
                                    $scope.scopeIdList.push(el.eventId);
                                    console.log($scope.scopeIdList)
                                    console.log(el.eventId)
                                }
                            },0);
                        }
                        for (var i = 0; i < data.length; i++) {
                            if (data[i].name.toLowerCase().indexOf(_research.toLowerCase()) > -1) {
                                uploadEvents(data[i]);
                            }
                        }
                        $scope.loadingMore = false;
                        $http.get('/artists/containing/'+_research).
                            success(function(data, status, headers, config) {
                                function getArtistEvents (art) {
                                    $http.get('/artists/'+ art.facebookUrl + '/events ').
                                        success(function(data){
                                            data.forEach(uploadEvents);
                                            $rootScope.resizeImgHeight()
                                        })
                                }
                                data.forEach(getArtistEvents)
                                $scope.loadingMore = false;
                            });
                        $http.get('/genres/'+ _research +'/20/' + offset + '/events ').
                            success(function(data, status, headers, config) {
                                data.forEach(uploadEvents);
                                console.log(data)
                                $rootScope.resizeImgHeight()
                            });
                        $http.get('/places/containing/'+_research).
                            success(function(data, status, headers, config) {
                                function getPlaceEvents (place) {
                                    $http.get('/places/'+ place.placeId + '/events ').
                                        success(function(data){
                                            console.log(data)
                                            data.forEach(uploadEvents);
                                            $rootScope.resizeImgHeight()
                                        })
                                }
                                data.forEach(getPlaceEvents)
                            });
                        $rootScope.resizeImgHeight()
                        $scope.loadingMore = false;
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
        }
    }
    function searchArtistFb () {
        $scope.artistsFb = $filter('filter')($scope.artistsFb, {name :  _research});
        console.log(_research.length)
        if ($scope.artistsFb.length < $scope.limit && _selArtist == true && _research.length > 1) {
            $scope.loadingFbArt = true;
            console.log('yo')
            $http.get('/artists/facebookContaining/'+_research)
                /*.start(function (data, etc) {
                    console.log("Dude! We're goin'!", data, etc);
                })
                /*.node('champ.*', function (value) {
                    $scope.items.push(value);
                })*/
                .success(function (value) {
                    $scope.loadingFbArt = false;
                    console.log(value)
                    var artistFbIdList = [];
                    function updateArtistFb (artistInfo) {
                        function getArtistFbIdInArtists (el) {
                            artistFbIdList.push(el.facebookId);
                        }
                        $scope.artists.forEach(getArtistFbIdInArtists);
                        $scope.artistsFb.forEach(getArtistFbIdInArtists);
                        if (artistFbIdList.indexOf(artistInfo.facebookId) < 0) {
                            console.log(artistInfo);
                            artistInfo.tracks = [];
                            artistFbIdList.push(artistInfo.facebookId);
                            $scope.artistsFb.push(artistInfo);
                            $rootScope.resizeImgHeight();
                            console.log($scope.artistsFb)
                        } else {
                        }
                    }
                    value.forEach(updateArtistFb);
                })
                .error(function (error) {
                    $scope.loadingFbArt = false;
                    console.log("Error: ", error);
                });
        }
    }
    search();
    if (_selArtist == true && _research.length > 2) {
        searchArtistFb ();
    }
    console.log($rootScope.geoLoc);
    $scope.moreLimit = function () {
        offset = offset + 20;
        $scope.loadingMore = true;
        $rootScope.resizeImgHeight();
        search();
    };
    var typingTimer;
    var doneTypingInterval = 600;
    $scope.research = function(newName) {
        if (angular.isDefined(newName)) {
            _research = newName;
            $scope.searchPat = newName;
            $scope.limit = 20;
            offset = 0;
            if (_selArtist == true && _research.length > 2) {
                clearTimeout(typingTimer);
                typingTimer = setTimeout(searchArtistFb, doneTypingInterval);
            }
            search();
        }
        return _research;
    };
    $scope.selArtist = function(newName) {
        if (angular.isDefined(newName)) {
            _selArtist = newName;
            search();
            searchArtistFb();
            $scope.limit = 20;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            }
        }
        return _selArtist;
    };
    $scope.selEvent = function(newName) {
        if (angular.isDefined(newName)) {
            _selEvent = newName;
            search();
            $scope.limit = 20;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            }
        }
        return _selEvent;
    };
    $scope.selPlace = function(newName) {
        if (angular.isDefined(newName)) {
            _selPlace = newName;
            search();
            $scope.limit = 20;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            }
        }
        return _selPlace;
    };
    $scope.selUsr = function(newName) {
        if (angular.isDefined(newName)) {
            _selUsr = newName;
            search();
            $scope.limit = 20;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            }
        }
        return _selUsr;
    };
    var StartTimer;
    var doneStartInterval = 600;
    $scope.selStart = function(newName) {
        if (angular.isDefined(newName)) {
            if (newName > 23 && newName <= 38) {
                newName = (newName-23)*24
            } else if (newName > 38 && newName <= 40) {
                newName = (newName-36)*168;
            } else if (newName > 40) {
                newName = (newName-39)*720;
            }
            console.log(newName)
            $rootScope.changeTimeEventView(newName);
            _selStart = newName;
            clearTimeout(StartTimer);
            StartTimer = setTimeout(search, doneStartInterval);                
            $scope.limit = 20;
            offset = 0;
            $scope.loadingMore = true;
        }
        return _selStart;
    };
}]);