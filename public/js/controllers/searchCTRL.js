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
                function getArtistId(el, index, array) {
                    scopeIdList.push(el.artistId);
                }
                $scope.artistes.forEach(getArtistId);
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
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
                if ($scope.artistes.length < $scope.limit) {
                    /*$scope.artistesFb = $filter('filter')($scope.artistesFb, {name :  _research});
                    var artistFbIdList = [];
                    function getArtistFbId(el) {
                        artistFbIdList.push(el.artistId);
                    }
                    function getArtistFbIdInArtists (el) {
                        artistFbIdList.push(el.facebookId);
                    }
                    $scope.artistesFb.forEach(getArtistFbId);
                    $scope.artistes.forEach(getArtistFbIdInArtists);*/
                    $http.get('https://graph.facebook.com/v2.2/search?q='+ _research + '&limit=200&type=page&fields=name,cover,id,category,likes,link,website&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                        success(function(data, status, headers, config) {
                            $scope.data = data.data;
                            //if ($scope.artistesFb.length == 0) {
                                $scope.artistesFb = [];
                            //}
                            function updateArtistFb (el, index, array) {
                                if (el.category == 'Musician/band' && el.cover != undefined) {
                                    var newArtist =[];
                                    newArtist.artistId = el.id;
                                    newArtist.name = el.name;
                                    newArtist.likes = el.likes;
                                    newArtist.link = el.link;
                                    newArtist.website = el.website;
                                    newArtist.images =[];
                                    newArtist.images.push({path: el.cover.source});
                                    newArtist.soundcloud = [];
                                    newArtist.tracks = [];
                                    SC.initialize({
                                        client_id: 'f297807e1780623645f8f858637d4abb'
                                    });
                                    if (newArtist.website != undefined) {
                                        if (soundcloudUrl = newArtist.website.match(/soundcloud\W.*/)) {
                                            soundcloudUrl = soundcloudUrl[0].split(" ");
                                            for (var urls=0; urls < soundcloudUrl.length; urls++) {
                                                var soundcloudNameMatched = soundcloudUrl[urls].substring(soundcloudUrl[urls].indexOf(".com/") + 5)
                                                $http.get('http://api.soundcloud.com/users/' + soundcloudNameMatched + '/tracks?client_id=f297807e1780623645f8f858637d4abb').
                                                    success(function (data, status, headers, config) {
                                                        function addTrack(track) {
                                                            var newTrack = [];
                                                            newTrack.url = track.stream_url;
                                                            newTrack.name = track.title;
                                                            newTrack.from = 'soundcloud';
                                                            newTrack.image = track.artwork_url;
                                                            newTrack.artist = newArtist.name;
                                                            newArtist.tracks.push(newTrack);
                                                        }
                                                        data.forEach(addTrack);
                                                        
                                                    })
                                            }
                                        }
                                    } else {
                                        SC.get('/users', { q: "'" + el.name + "'"}, function (users) {
                                            function getSoundcloudName(elem) {
                                                function findSouncloudTracks() {
                                                    $http.get('http://api.soundcloud.com/users/' + newArtist.soundcloud.id + '/tracks?client_id=f297807e1780623645f8f858637d4abb').
                                                        success(function (data, status, headers, config) {
                                                            function addTrack(track) {
                                                                console.log(track)
                                                                var newTrack = [];
                                                                newTrack.url = track.stream_url;
                                                                newTrack.name = track.title;
                                                                newTrack.from = 'soundcloud';
                                                                newTrack.image = track.artwork_url;
                                                                newTrack.artist = newArtist.name;
                                                                newArtist.tracks.push(newTrack);
                                                            }

                                                            data.forEach(addTrack);
                                                        })
                                                }

                                                $http.get('http://api.soundcloud.com/users/' + elem.id + '/web-profiles?client_id=f297807e1780623645f8f858637d4abb').
                                                    success(function (data, status, headers, config) {
                                                        function getSouncloudUser(link) {
                                                            var matchedId = link.url.match(/\d.*/);
                                                            if (matchedId == null) {
                                                                matchedId = ['0'];
                                                            }
                                                            if (el.website == undefined) {
                                                                el.website = 'empty';
                                                            }
                                                            if (link.url == el.link || matchedId[0].indexOf(el.id) > -1 || el.website.indexOf(link.url) > -1 || el.website.indexOf(elem.id) > -1) {
                                                                if (newArtist.soundcloud.length == 0) {
                                                                    newArtist.soundcloud = elem;
                                                                    findSouncloudTracks()
                                                                } else if (elem.followers_count > newArtist.soundcloud.followers_count) {
                                                                    newArtist.soundcloud = elem;
                                                                    findSouncloudTracks()

                                                                }
                                                            }
                                                        }

                                                        data.forEach(getSouncloudUser)
                                                    });
                                            }

                                            users.forEach(getSoundcloudName)
                                        });
                                    }
                                    $scope.artistesFb.push(newArtist);
                                }
                            }
                            for (var ii = 0; ii < $scope.data.length; ii++) {
                                if ($scope.artistesFb.length < $scope.limit /*&& artistFbIdList.indexOf($scope.data[ii].id) == -1*/) {
                                    updateArtistFb($scope.data[ii])
                                } else {
                                    return
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
                function getPlaceId(el, index, array) {
                    scopeIdList.push(el.placeId);
                }
                $scope.users.forEach(getPlaceId);
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
                        console.log(data);
                        if ($scope.events.length == 0) {
                            $scope.events = data;
                        } else {
                            function uploadEvents(el, index, array) {
                                if (scopeIdList.indexOf(el.eventId) == -1) {
                                    $scope.events.push(el);
                                }
                            }
                            data.forEach(uploadEvents);
                        }
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
        }
    }
    $scope.createArtist = function (artist) {
        console.log(artist)
        $http.post('artists/createArtist', {artistName : artist.name, facebookId: artist.id}).
            success(function(data, status, headers, config) {
                window.location.href =('#/artiste/' + data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
}]);