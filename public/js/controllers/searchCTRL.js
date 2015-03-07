app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$filter', 'oboe', function($rootScope, $http, $scope, $filter, oboe){
    $scope.limit = 12;
    $scope.artists = [];
    $scope.artistsFb = [];
    $scope.users = [];
    $scope.places = [];
    $scope.events = [];
    var _selArtist = $rootScope.activArtist;
    var _selEvent = $rootScope.activEvent;
    var _selUsr = $rootScope.activUsr;
    var _selPlace = $rootScope.activPlace;
    var _research = '';
    $scope.moreLimit = function () {
        $scope.limit = $scope.limit + 12;
        $rootScope.resizeImgHeight()
    };
    function search () {
        $rootScope.activArtist = _selArtist;
        $rootScope.activEvent = _selEvent;
        $rootScope.activPlace = _selPlace;
        $rootScope.activUsr = _selUsr;
        if (_research.length == 0) {
            if (_selEvent == true) {
                $http.get('/events').
                    success(function (data, status, headers, config) {
                        if (data != $scope.events) {
                            $scope.events = data;
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
                        if (data != $scope.artists) {
                            $scope.artists = data;
                            $rootScope.resizeImgHeight()
                        }
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
                $http.get('/artists/containing/'+_research).
                    success(function(data, status, headers, config) {
                        $scope.artists = data;
                        $rootScope.resizeImgHeight();
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
                $scope.artistsFb = $filter('filter')($scope.artistsFb, {name :  _research});
            }
            if (_selPlace == true) {
                $scope.places = $filter('filter')($scope.places, {name :  _research});
                var scopeIdList = [];
                function getPlaceId(el, index, array) {
                    scopeIdList.push(el.placeId);
                }
                $scope.places.forEach(getPlaceId);
                $http.get('/places/containing/'+_research).
                    success(function(data, status, headers, config) {
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
                $scope.users = $filter('filter')($scope.users, {nickname :  _research});
                var scopeIdList = [];
                function getUsersId(el, index, array) {
                    scopeIdList.push(el.userId);
                }
                $scope.users.forEach(getUsersId);
                $http.get('/users/containing/'+_research).
                    success(function(data, status, headers, config) {
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
                var scopeIdList = [];
                $scope.events = $filter('filter')($scope.events, {name :  _research});
                function getEventsId(el, index, array) {
                    scopeIdList.push(el.eventId);
                }
                $scope.events.forEach(getEventsId);
                $http.get('/events/containing/' + _research).
                    success(function (data, status, headers, config) {
                        if ($scope.events.length == 0) {
                            $scope.events = data;
                        } else {
                            function uploadEvents(el, index, array) {
                                if (scopeIdList.indexOf(el.eventId) == -1) {
                                    $scope.events.push(el);
                                    console.log(el)
                                }
                            }
                            data.forEach(uploadEvents);
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
            function getArtistFbId(el) {
                artistFbIdList.push(el.facebookId);
            }
            function getArtistFbIdInArtists (el) {
                artistFbIdList.push(el.facebookId);
            }
            $scope.artistsFb.forEach(getArtistFbId);
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
                        if ($scope.artistsFb.indexOf(artistInfo) < 0) {
                            console.log(artistInfo);
                            artistInfo.tracks = [];
                            $scope.artistsFb.push(artistInfo);
                            $scope.$apply();
                            $rootScope.resizeImgHeight();
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