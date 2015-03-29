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
    var _selStart = $rootScope.maxStart;
    if (document.getElementById('searchBar') != null) {
        var _research = document.getElementById('searchBar').value.trim();
    } else {
        var _research = '';
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
                var maxStartTime =  _selStart + new Date().getTime();
                for (var e = 0; e < eventsLenght; e++) {
                    console.log(maxStartTime)
                    if ($scope.events[e].startTime > maxStartTime) {
                        $scope.events.splice(e, 1)
                        $scope.$apply();
                        e = e -1;
                    }
                }
                $http.get('/events/offsetAndMaxStartTime/'+ offset+ '/' + $rootScope.geoLoc + '/' + _selStart/3600000).
                    success(function (data, status, headers, config) {
                        var scopeIdList = [];
                        function getEventId(el, index, array) {
                            scopeIdList.push(el.eventId);
                        }
                        $scope.events.forEach(getEventId);
                        if ($scope.events.length == 0) {
                            $scope.events = data;
                        } else {
                            function uploadEvents(el, index, array) {
                                if (scopeIdList.indexOf(el.eventId) == -1) {
                                    $scope.events.push(el);
                                }
                            }
                            data.forEach(uploadEvents)
                        }
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
                $http.get('/artists').
                    success(function (data, status, headers, config) {
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
                        if ($scope.places.length == 0) {
                            $scope.places = data;
                        } else {
                            function uploadPlaces(el, index, array) {
                                if (scopeIdList.indexOf(el.placeId) == -1) {
                                    $scope.places.push(el);
                                }
                            }
                            data.forEach(uploadPlaces)
                        }
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
                        if ($scope.places.length == 0) {
                            $scope.places = data;
                        } else {
                            function uploadPlaces(el, index, array) {
                                if (scopeIdList.indexOf(el.placeId) == -1) {
                                    $scope.places.push(el);
                                }
                            }
                            data.forEach(uploadPlaces)
                        }
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
            _research = newName
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
            _selStart = newName;
            clearTimeout(StartTimer);
            StartTimer = setTimeout(search, doneStartInterval);                
            $scope.limit = 20;
            offset = 0;
        }
        return _selStart;
    };
}]);