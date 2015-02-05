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
    function location() {
        console.log($location.path());
        if ($location.path() == '/' || $location.path() == '/search') {
            $rootScope.home = true;
            $rootScope.pathArt = false;
            $rootScope.pathEvent = false;
            $rootScope.pathUsr = false;
        } else if ($location.path().indexOf('/artiste') > -1){
            $rootScope.pathArt = true;
            $rootScope.home = false;
            $rootScope.pathEvent = false;
            $rootScope.pathUsr = false;
        } else if ($location.path().indexOf('/event') > -1){
            rootScope.pathEvent = true;
            $rootScope.pathArt = false;
            $rootScope.home = false;
            $rootScope.pathUsr = false;
        } else if ($location.path().indexOf('/user') > -1){
            $rootScope.pathUsr = true;
            $rootScope.pathArt = false;
            $rootScope.pathEvent = false;
            $rootScope.home = false;
        }
    }
    location();
    $scope.$on('$locationChangeSuccess', function(){
        location()
    });
    var _research = '';
    $scope.research = function(newName) {
        if (angular.isDefined(newName)) {
            _research = newName;
            console.log("Setting research var, launch search method");
            search();
        }
        return _research;
    };
    $rootScope.limit = 12;
    $rootScope.moreLimit = function () {
        $rootScope.limit = $rootScope.limit + 12;
    };
    $rootScope.selAll = true;
    $rootScope.selEvent = true;
    $rootScope.selArtist = true;
    $rootScope.selPlace = true;
    $rootScope.selUsr = true;
    $rootScope.artistes = [];
    $rootScope.artistesFb = [];
    $rootScope.users = [];
    $rootScope.places = [];
    $rootScope.eventsBase = [];
    $scope.events = $http.get('/events').
        success(function(data, status, headers, config) {
            //console.log(data);
            $rootScope.events = data;
            $rootScope.eventsBase = data;
        }).
        error(function(data, status, headers, config) {
            // called asynchronously if an error occurs
            // or server returns response with an error status.
        });
    $rootScope.$watch('selAll', function(newVal, oldVal){
        console.log(newVal + "//" + oldVal);
        console.log($rootScope.selArtist);
        if (newVal == true) {
            $rootScope.selEvent = true;
            $rootScope.selArtist = true;
            $rootScope.selPlace = true;
            $rootScope.selUsr = true;
        }
        search()
    });
    $rootScope.$watch('selEvent', function(newVal, oldVal){
        if (newVal == false) {
            $rootScope.selAll = false;
        } else if($rootScope.selAll == false) {
            $rootScope.selArtist = false;
            $rootScope.selUsr = false;
            $rootScope.selPlace = false;
        }
        search()
    });
    $rootScope.$watch('selArtist', function(newVal, oldVal){
        if (newVal == false) {
            $rootScope.selAll = false;
        } else if($rootScope.selAll == false) {
            $rootScope.selEvent = false;
            $rootScope.selUsr = false;
            $rootScope.selPlace = false;
        }
        search()
    });
    $rootScope.$watch('selPlace', function(newVal, oldVal){
        if (newVal == false) {
            $rootScope.selAll = false;
        } else if($rootScope.selAll == false) {
            $rootScope.selEvent = false;
            $rootScope.selUsr = false;
            $rootScope.selArtist = false;
        }
        search()
    });
    $rootScope.$watch('selUsr', function(newVal, oldVal){
        if (newVal == false) {
            $rootScope.selAll = false;
        } else if($rootScope.selAll == false) {
            $rootScope.selEvent = false;
            $rootScope.selPlace = false;
            $rootScope.selArtist = false;
        }
        search()
    });

    function search (){
        if (_research.length == 0) {
            $rootScope.events = $rootScope.eventsBase;
            $rootScope.artistes = [];
            $rootScope.artistesFb = [];
            $rootScope.users = [];
            $rootScope.places = [];
        } else {
            if ($rootScope.selArtist == true) {
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
            if ($rootScope.selPlace == true) {
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
            if ($rootScope.selUsr == true) {
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
            if ($rootScope.selEvent == true) {
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
     $rootScope.createPlaces = function () {
        var places = [];
        var placeName =[];
        var txtFile = new XMLHttpRequest();
        txtFile.open("GET", "/assets/json/salles.txt", true);
        txtFile.onreadystatechange = function()
        {
            if (txtFile.readyState === 4) {  // document is ready to parse.
                if (txtFile.status === 200) {  // file is found
                    allText = txtFile.responseText;
                    lines = txtFile.responseText.split("\n");
                    for (var l=0; l<lines.length; l++) {
                        if (lines[l] == "Salles de 400 à 1200 places") {
                            placeName.push(lines[l-1].replace(/ /g, "+"));
                            console.log(placeName)
                            getPlacesByName(lines[l-1].replace(/ /g, "+"))
                        }
                    }
                }
            }
        };
        txtFile.send(null);
        function getPlacesByName(placeName) {
            $http.get('https://graph.facebook.com/v2.2/search?q=' + placeName + '&type=page&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function (data, status, headers, config) {
                    data = data.data;
                    for (var iv = 0; iv < data.length; iv++) {
                        if (data[iv].category == 'Concert venue' || data[iv].category == 'Club') {
                            getPlacesById(data[iv]);
                        } else if (data[iv].category_list != undefined) {
                            for (ii = 0; ii < data[iv].category_list.length; ii++) {
                                if (data[iv].category_list[ii].name == 'Concert Venue' || data[iv].category_list[ii].name == 'Club') {
                                    getPlacesById(data[iv]);
                                }
                            }
                        }
                    }
                }).
                error(function (data, status, headers, config) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
        function getPlacesById (searchPlaces) {
            $http.get('https://graph.facebook.com/v2.2/'+ searchPlaces.id +'/?fields=checkins,cover,description,hours,id,likes,link,location,name,phone,website,picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function(data, status, headers, config) {
                    flag = 0;
                    for (m = 0; m < places.length; m++) {
                        if (places[m].id == data.id){
                            flag = 1;
                        } else if (places[m].location.latitude == data.location.latitude && places[m].location.longitude == data.location.longitude && places[m].likes > data.likes) {
                            flag = 1;
                        } else if (places[m].location.latitude == data.location.latitude && places[m].location.longitude == data.location.longitude && places[m].likes < data.likes) {
                            places.splice(m, 1);
                        }
                    }
                    if (data.location.country != undefined && data.location.country != 'France') {
                        flag = 1;
                    }
                    if (flag == 0){
                        var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;;
                        data.description = data.description.replace(/(\n\n)/g, " <br/><br/></div><div class='column large-12'>");
                        data.description = data.description.replace(/(\n)/g, " <br/>");
                        if (matchedLinks = data.description.match(links)) {
                            var m = matchedLinks;
                            var unique = [];
                            for (var ii = 0; ii < m.length; ii++) {
                                var current = m[ii];
                                if (unique.indexOf(current) < 0) unique.push(current);
                            }
                            console.log(unique);
                            for (var i=0; i < unique.length; i++) {
                                data.description = data.description.replace(new RegExp(unique[i],"g"),
                                        "<a href='" + unique[i]+ "'>" + unique[i] + "</a>")
                            }
                        }
                        $http.post('/places/create', {
                            name: data.name,
                            facebookId: data.id,
                            capacity: data.checkins,
                            description:data.description,
                            webSite:data.website
                        })
                    }
                }).
                error(function(data, status, headers, config) {
                    console.log(data);
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
    }
}]);