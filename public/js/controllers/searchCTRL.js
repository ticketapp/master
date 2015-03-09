app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$filter', 'oboe', function($rootScope, $http, $scope, $filter, oboe){
    $scope.limit = 12;
    $scope.artists = [];
    $scope.artistsFb = [];
    $scope.users = [];
    $scope.places = [];
    $scope.events = [];
    var offset = 0;
    var _selArtist = $rootScope.activArtist;
    var _selEvent = $rootScope.activEvent;
    var _selUsr = $rootScope.activUsr;
    var _selPlace = $rootScope.activPlace;
    var _research = '';
    function search () {
        $rootScope.activArtist = _selArtist;
        $rootScope.activEvent = _selEvent;
        $rootScope.activPlace = _selPlace;
        $rootScope.activUsr = _selUsr;
        $scope.filterSearch = _research;
        if (_research.length == 0) {
            if (_selEvent == true) {
                $http.get('/events/offset/' + offset).
                    success(function (data, status, headers, config) {
                        if (data != $scope.events) {
                            $scope.events = $scope.events.concat(data);;
                            console.log($scope.events)
                        }
                        $rootScope.resizeImgHeight()
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
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selPlace == true) {
                $http.get('/places').
                    success(function(data, status, headers, config) {
                        if (data != $scope.places) {
                            $scope.places = data;
                        }
                        $rootScope.resizeImgHeight()
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
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selEvent == true) {
                $http.get('/events/containing/' + _research + '/' + $rootScope.geoLoc).
                    success(function (data, status, headers, config) {
                        var scopeIdList = [];
                        $scope.events = $filter('filter')($scope.events, {name :  _research});
                        function getEventsId(el, index, array) {
                            scopeIdList.push(el.eventId);
                            console.log(scopeIdList)
                        }
                        $scope.events.forEach(getEventsId);
                        if ($scope.events.length == 0) {
                            $scope.events = data;
                        } else {
                            function uploadEvents(el, index, array) {
                                if (scopeIdList.indexOf(el.eventId) == -1) {
                                    $scope.events.push(el);
                                    scopeIdList.push(el.eventId);
                                    console.log(el)
                                } else {
                                    console.log('yoyo')
                                }
                                console.log(scopeIdList)
                            }
                            for (var i = 0; i < data.length; i++) {
                                uploadEvents(data[i]);
                            }
                        }
                        $rootScope.resizeImgHeight()
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
        if ($scope.artists.length + $scope.artistsFb.length < $scope.limit && _selArtist == true) {
            var artistFbIdList = [];
            function getArtistFbIdInArtists (el) {
                artistFbIdList.push(el.facebookId);
            }
            $scope.artists.forEach(getArtistFbIdInArtists);
            oboe.get('/artists/facebookContaining/'+_research)
                .start(function (data, etc) {
                    console.log("Dude! We're goin'!", data, etc);
                })
                /*.node('champ.*', function (value) {
                    $scope.items.push(value);
                })*/
                .done(function (value) {
                    function updateArtistFb (artistInfo) {
                        if ($scope.artistsFb.indexOf(artistInfo) < 0 && artistFbIdList.indexOf(artistInfo.facebookId) < 0) {
                            console.log(artistInfo);
                            artistInfo.tracks = [];
                            $scope.artistsFb.push(artistInfo);
                            $scope.$apply();
                            $rootScope.resizeImgHeight();
                            console.log($scope.artistsFb)
                        } else {
                            console.log('yoyo')
                        }
                    }
                    value.forEach(updateArtistFb);
                })
                .fail(function (error) {
                    console.log("Error: ", error);
                });
        }
    }
    search();
    $scope.moreLimit = function () {
        $scope.limit = $scope.limit + 12;
        offset = offset + 20;
        $rootScope.resizeImgHeight();
        search();
    };
    var typingTimer;
    var doneTypingInterval = 600;
    $scope.research = function(newName) {
        if (angular.isDefined(newName)) {
            _research = newName;
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
        }
        return _selArtist;
    };
    $scope.selEvent = function(newName) {
        if (angular.isDefined(newName)) {
            _selEvent = newName;
            search();
        }
        return _selEvent;
    };
    $scope.selPlace = function(newName) {
        if (angular.isDefined(newName)) {
            _selPlace = newName;
            search();
        }
        return _selPlace;
    };
    $scope.selUsr = function(newName) {
        if (angular.isDefined(newName)) {
            _selUsr = newName;
            search();
        }
        return _selUsr;
    };
}]);