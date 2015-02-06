app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$location', function($rootScope, $http, $scope, $location){
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
            console.log("Setting research var, launch search method");
            search();
        }
        return _research;
    };
    var _selArtist = $rootScope.activArtist;
    $scope.selArtist = function(newName) {
        if (angular.isDefined(newName)) {
            _selArtist = newName;
            console.log("Setting research var, launch search method");
            console.log(_selArtist)
            search();
        }
        return _selArtist;
    };
    var _selEvent = $rootScope.activEvent;
    $scope.selEvent = function(newName) {
        if (angular.isDefined(newName)) {
            _selEvent = newName;
            console.log("Setting research var, launch search method");
            search();
        }
        return _selEvent;
    };
    var _selPlace = $rootScope.activPlace;
    $scope.selPlace = function(newName) {
        if (angular.isDefined(newName)) {
            _selPlace = newName;
            console.log("Setting research var, launch search method");
            search();
        }
        return _selPlace;
    };
    var _selUsr = $rootScope.activUsr;
    $scope.selUsr = function(newName) {
        if (angular.isDefined(newName)) {
            _selUsr = newName;
            console.log("Setting research var, launch search method");
            search();
        }
        return _selUsr;
    };
    $rootScope.limit = 12;
    $rootScope.moreLimit = function () {
        $rootScope.limit = $rootScope.limit + 12;
    };
    $rootScope.artistes = [];
    $rootScope.artistesFb = [];
    $rootScope.users = [];
    $rootScope.places = [];
    $rootScope.events = [];
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
                            $rootScope.events = data;
                            $rootScope.eventsBase = data;
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selArtist == true) {
                $http.get('/artists').
                    success(function (data, status, headers, config) {
                        $rootScope.artistes = data;
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selUsr == true) {
                $http.get('/users').
                    success(function(data, status, headers, config) {
                        //console.log(data);
                        $rootScope.users = data;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selPlace == true) {
                $http.get('/places').
                    success(function(data, status, headers, config) {
                        //console.log(data);
                        $rootScope.places = data;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            $rootScope.artistesFb = [];
        } else {
            if (_selArtist == true) {
                console.log("artist")
                $http.get('/artists/startWith/'+_research).
                    success(function(data, status, headers, config) {
                        //console.log(data);
                        $rootScope.artistes = data;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
                if ($rootScope.artistes.length < $rootScope.limit) {
                    console.log("artistFb")
                    $http.get('https://graph.facebook.com/v2.2/search?q='+ _research + '&limit=200&type=page&fields=name,cover,id,category,likes&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                        success(function(data, status, headers, config) {
                            $rootScope.artistesFb = [];
                            //$rootScope.artistesFb = data.data;
                            $rootScope.data = data.data;
                            var flag = 0;
                            for (var i=0; i < $rootScope.data.length; i++) {
                                if ($rootScope.data[i].category == 'Musician/band') {
                                    for (var j=0; j < $rootScope.artistes.length; j++) {
                                        if($rootScope.artistes[j].facebookId == $rootScope.data[i].id) {
                                            flag = 1;
                                            break;
                                        }
                                    }
                                    if(flag == 0) {
                                        var newArtist =[];
                                        newArtist.artistId = $rootScope.data[i].id;
                                        newArtist.name =$rootScope.data[i].name;
                                        newArtist.likes =$rootScope.data[i].likes;
                                        newArtist.images =[];
                                        newArtist.images.push({path: $rootScope.data[i].cover.source});
                                        $rootScope.artistesFb.push(newArtist);
                                        console.log($rootScope.artistesFb);
                                        if ($rootScope.artistesFb.length == $rootScope.limit + 12 || $rootScope.artistes.length == $rootScope.limit + 24) {
                                            console.log($rootScope.artistesFb)
                                            break;
                                        }
                                    } else {
                                        flag = 0;
                                    }
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
                $http.get('/places/startWith/'+_research).
                    success(function(data, status, headers, config) {
                        //console.log(data);
                        $rootScope.places = data;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            console.log('u' + _selUsr +'/ a' + _selArtist + '/ e' + _selEvent + '/ p' + _selPlace);
            if (_selUsr == true) {
                $http.get('/users/startWith/'+_research).
                    success(function(data, status, headers, config) {
                        //console.log(data);
                        $rootScope.users = data;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selEvent == true) {
                $http.get('/events/startWith/' + _research).
                    success(function (data, status, headers, config) {
                        //console.log(data);
                        $rootScope.events = data;
                    }).
                    error(function (data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
        }
    }
    $rootScope.GetArtisteById = function(id){
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
            success(function(data, status, headers, config) {
                $rootScope.artiste = data;
                createArtiste(data)
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };

    function createArtiste () {
        $http.post('artists/createArtist', {artistName : $rootScope.artiste.name, facebookId: $rootScope.artiste.id}).
            success(function(data, status, headers, config) {
                window.location.href =('#/artiste/' + data);
                console.log(data)
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
}]);