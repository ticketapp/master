app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$filter', function($rootScope, $http, $scope, $filter){
    $scope.limit = 12;
    $scope.artistes = [];
    $scope.artistesFb = [];
    $scope.users = [];
    $scope.places = [];
    $scope.events = [];
    var _selArtist = $rootScope.activArtist;
    var _selEvent = $rootScope.activEvent;
    var _selUsr = $rootScope.activUsr;
    var _selPlace = $rootScope.activPlace;
    var _research = '';
    function imgHeight () {
        var waitForContentMin = setTimeout(function () {
            var content = document.getElementsByClassName('img_min_evnt');
            if (content.length > 0) {
                clearInterval(waitForContentMin);
                var newHeight = content[0].clientWidth * 0.376 + 'px';
                for (var i = 0; i < content.length; i++) {
                    content[i].style.height = newHeight;
                }
            }
        }, 100)
    }
    $scope.moreLimit = function () {
        $scope.limit = $scope.limit + 12;
        imgHeight()
    };
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
                        imgHeight()
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
                        imgHeight()
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
                        imgHeight()
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
                        imgHeight()
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
                var scopeIdList = $scope.artistes.map(function(artist) {
                    return artist.artistId;
                });

                $http.get('/artists/containing/'+_research).
                    success(function(data, status, headers, config) {
                        if ($scope.artistes.length == 0) {
                            $scope.artistes = data;
                        }
                        function uploadArtistes(el, index, array) {
                            if (scopeIdList.indexOf(el.artistId) == -1) {
                                $scope.artistes.push(el);
                            }
                        }
                        data.forEach(uploadArtistes);
                        imgHeight()
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
                $scope.artistesFb = $filter('filter')($scope.artistesFb, {name :  _research});
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
                        } else {;
                            function uploadPlaces(el, index, array) {
                                if (scopeIdList.indexOf(el.placeId) == -1) {
                                    $scope.places.push(el);
                                }
                            }
                            data.forEach(uploadPlaces)
                        }
                        imgHeight()
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
                                if (scopeIdList.indexOf(el.userId) == -1) {
                                    $scope.users.push(el);
                                }
                            }
                            data.forEach(uploadUsers);
                        }
                        imgHeight()
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
                        imgHeight()
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
        }
    }
    function searchArtistFb () {
        if ($scope.artistes.length + $scope.artistesFb.length < $scope.limit && _selArtist == true) {
            var artistFbIdList = [];
            function getArtistFbId(el) {
                artistFbIdList.push(el.artistId);
            }
            function getArtistFbIdInArtists (el) {
                artistFbIdList.push(el.facebookId);
            }
            $scope.artistesFb.forEach(getArtistFbId);
            $scope.artistes.forEach(getArtistFbIdInArtists);
            $http.get('/artists/facebookContaining/'+_research).
                success(function(data, status, headers, config) {
                    if ($scope.artistesFb.length == 0) {
                        $scope.artistesFb = data;
                    } else {
                        function uploadArtistesFb (el, index, array) {
                            if (artistFbIdList.indexOf(el.artistId) == -1) {
                                $scope.artistesFb.push(el);
                            }
                        }
                        data.forEach(uploadArtistesFb);
                    }
                    var artistsFbLength = $scope.artistesFb.length;
                    for (var i = 0; i < artistsFbLength; i++) {
                        $scope.artistesFb[i].tracks = [];
                        var tracksSCLength = $scope.artistesFb[i].soundCloudTracks.length;
                        var tracksYTLength = $scope.artistesFb[i].youtubeTracks.length;
                        for (var ii =0; ii < tracksSCLength; ii++) {
                            var newTrack = $scope.artistesFb[i].soundCloudTracks[ii];
                            newTrack.from = 'soundcloud';
                            newTrack.url = $scope.artistesFb[i].soundCloudTracks[ii].stream_url;
                            newTrack.image = $scope.artistesFb[i].soundCloudTracks[ii].artwork_url;
                            newTrack.artist = $scope.artistesFb[i].name;
                            $scope.artistesFb[i].tracks.push(newTrack);
                        }
                        for (var v =0; v < tracksYTLength; v++) {
                            var newTrack = $scope.artistesFb[i].youtubeTracks[v];
                            newTrack.url = $scope.artistesFb[i].youtubeTracks[v].videoId;
                            newTrack.from = 'youtube';
                            newTrack.image = $scope.artistesFb[i].youtubeTracks[v].thumbnail;
                            newTrack.artist = $scope.artistesFb[i].name;
                            $scope.artistesFb[i].tracks.push(newTrack);
                        }
                        console.log($scope.artistesFb[i])
                        imgHeight()
                    }
                }).
                error(function(data, status, headers, config) {
                    console.log(data);
                });
        }
    }
    search();
    $scope.createArtist = function (artist) {
        $http.post('artists/createArtist', {artistName : artist.name, facebookId: artist.id}).
            success(function(data, status, headers, config) {
                window.location.href =('#/artiste/' + data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };
    var typingTimer;
    var doneTypingInterval = 1000;
    $scope.research = function(newName) {
        if (angular.isDefined(newName)) {
            _research = newName;
            if (_selArtist == true) {
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