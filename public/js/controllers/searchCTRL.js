app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$filter', function($rootScope, $http, $scope, $filter){
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position)
            {
                alert("Latitude : " + position.coords.latitude + ", longitude : " + position.coords.longitude);
            }, function erreurPosition(error) {
                }
        );
    } else {
    }
    var _research = '';
    $scope.research = function(newName) {
        if (angular.isDefined(newName)) {
            _research = newName;
            search();
        }
        return _research;
    };
    var _selArtist = $rootScope.activArtist;
    $scope.selArtist = function(newName) {
        if (angular.isDefined(newName)) {
            _selArtist = newName;
            search();
        }
        return _selArtist;
    };
    var _selEvent = $rootScope.activEvent;
    $scope.selEvent = function(newName) {
        if (angular.isDefined(newName)) {
            _selEvent = newName;
            search();
        }
        return _selEvent;
    };
    var _selPlace = $rootScope.activPlace;
    $scope.selPlace = function(newName) {
        if (angular.isDefined(newName)) {
            _selPlace = newName;
            search();
        }
        return _selPlace;
    };
    var _selUsr = $rootScope.activUsr;
    $scope.selUsr = function(newName) {
        if (angular.isDefined(newName)) {
            _selUsr = newName;
            search();
        }
        return _selUsr;
    };
    $scope.limit = 12;
    $scope.moreLimit = function () {
        $scope.limit = $scope.limit + 12;
    };
    $scope.artistes = [];
    $scope.artistesFb = [];
    $scope.users = [];
    $scope.places = [];
    $scope.events = [];
    search();

    function search (){
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
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selArtist == true) {
                $http.get('/artists').
                    success(function (data, status, headers, config) {
                        if (data != $scope.artistes) {
                            $scope.artistes = data;
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
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            $scope.artistesFb = [];
        } else {
            if (_selArtist == true) {
                $scope.artistes = $filter('filter')($scope.artistes, {name :  _research});
                var scopeIdList = [];
                $scope.artistes.forEach(getArtistId);
                function getArtistId(el, index, array) {
                    scopeIdList.push(el.artistId);
                }
                $http.get('/artists/containing/'+_research).
                    success(function(data, status, headers, config) {
                        if ($scope.artistes.length == 0) {
                            $scope.artistes = data;
                        }
                        data.forEach(uploadArtistes);
                        function uploadArtistes(el, index, array) {
                            if (scopeIdList.indexOf(el.artistId) == -1) {
                                $scope.artistes.push(el);
                            }
                        }
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
                if ($scope.artistes.length < $scope.limit) {
                    $scope.artistesFb = $filter('filter')($scope.artistesFb, {name :  _research});
                    var artistFbIdList = [];
                    $scope.artistesFb.forEach(getArtistFbId);
                    $scope.artistes.forEach(getArtistFbIdInArtists);
                    function getArtistFbId(el) {
                        artistFbIdList.push(el.artistId);
                    }
                    function getArtistFbIdInArtists (el) {
                        artistFbIdList.push(el.facebookId);
                    }
                    $http.get('https://graph.facebook.com/v2.2/search?q='+ _research + '&limit=200&type=page&fields=name,cover,id,category,likes&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                        success(function(data, status, headers, config) {
                            $scope.data = data.data;
                            if ($scope.artistesFb.length == 0) {
                                $scope.artistesFb = [];
                            }
                            $scope.data.forEach(updateArtistFb);
                            function updateArtistFb (el, index, array) {
                                if (artistFbIdList.indexOf(el.id) == -1 && el.category == 'Musician/band') {
                                    var newArtist =[];
                                    newArtist.artistId = el.id;
                                    newArtist.name = el.name;
                                    newArtist.likes = el.likes;
                                    newArtist.images =[];
                                    newArtist.images.push({path: el.cover.source});
                                    $scope.artistesFb.push(newArtist);
                                }
                            }
                        }).
                        error(function(data, status, headers, config) {
                            // called asynchronously if an error occurs
                            // or server returns response with an error status.
                        });
                }
            }
            if (_selPlace == true) {
                $scope.places = $filter('filter')($scope.places, {name :  _research});
                var scopeIdList = [];
                $scope.users.forEach(getPlaceId);
                function getPlaceId(el, index, array) {
                    scopeIdList.push(el.placeId);
                }
                $http.get('/places/containing/'+_research).
                    success(function(data, status, headers, config) {
                        if ($scope.places.length == 0) {
                            $scope.places = data;
                        } else {
                            data.forEach(uploadPlaces);
                            function uploadPlaces(el, index, array) {
                                if (scopeIdList.indexOf(el.placeId) == -1) {
                                    $scope.places.push(el);
                                }
                            }
                        }
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selUsr == true) {
                $scope.users = $filter('filter')($scope.users, {nickname :  _research});
                var scopeIdList = [];
                $scope.users.forEach(getUsersId);
                function getUsersId(el, index, array) {
                    scopeIdList.push(el.userId);
                }
                $http.get('/users/containing/'+_research).
                    success(function(data, status, headers, config) {
                        if ($scope.users.length == 0) {
                            $scope.users = data;
                        } else {
                            data.forEach(uploadUsers);
                            function uploadUsers(el, index, array) {
                                if (scopeIdList.indexOf(el.userId) == -1) {
                                    $scope.users.push(el);
                                }
                            }
                        }
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selEvent == true) {
                var scopeIdList = [];
                $scope.events = $filter('filter')($scope.events, {name :  _research});
                $scope.events.forEach(getEventsId);
                function getEventsId(el, index, array) {
                    scopeIdList.push(el.eventId);
                }
                $http.get('/events/containing/' + _research).
                    success(function (data, status, headers, config) {
                        console.log(data);
                        if ($scope.events.length == 0) {
                            $scope.events = data;
                        } else {
                            data.forEach(uploadEvents);
                            function uploadEvents(el, index, array) {
                               if (scopeIdList.indexOf(el.eventId) == -1) {
                                   $scope.events.push(el);
                               }
                            }
                        }
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
        }
    }
    $scope.GetArtisteById = function(id){
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
            success(function(data, status, headers, config) {
                $scope.artiste = data;
                createArtiste(data)
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };

    function createArtiste () {
        $http.post('artists/createArtist', {artistName : $scope.artiste.name, facebookId: $scope.artiste.id}).
            success(function(data, status, headers, config) {
                window.location.href =('#/artiste/' + data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
}]);