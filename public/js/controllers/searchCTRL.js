app.controller('searchCtrl', ['$scope', '$http', '$filter', function($scope, $http, $filter){
    $scope.research = "";
    $scope.artistes = [];
    $scope.search = function(){
        $http.get('/artists/'+$scope.research).
            success(function(data, status, headers, config) {
                //console.log(data);
                $scope.artistes = data;
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('/events/'+$scope.research).
            success(function(data, status, headers, config) {
                //console.log(data);
                $scope.events = data;
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('/places/'+$scope.research).
            success(function(data, status, headers, config) {
                //console.log(data);
                $scope.places = data;
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('/users/'+$scope.research).
            success(function(data, status, headers, config) {
                //console.log(data);
                $scope.users = data;
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('http://developer.echonest.com/api/v4/artist/search?api_key=3ZYZKU3H3MKR2M59Z&format=json&name=madben&bucket=biographies&bucket=songs&bucket=').
            success(function(data, status, headers, config) {
                console.log(data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.research + '&limit=200&type=page&access_token=CAACEdEose0cBABQgC9YWL113gkHLrA9pgDN38FUK8RJvFAjylDSKtUXHbDLjcLUAlbzh77LNvYwiM8GzCZAes0zUnGEc6y1VchDIXcObwIuPR9GtPEwvvys6irhtDoQKy7ObDe1uHVCm2R7ajSfPKVet2AXMYWVveOw6crs6Oc0ZAh6om5VUYK0HMuxhJGaX9toePrJZAfC1BqmwFZBLeMTAY4gwZBKoZD').
            success(function(data, status, headers, config) {
                //$scope.artistesFb = data.data;
                $scope.data = data.data;
                $scope.artistesFb = [];
                var flag = 0;
                for (var i=0; i < $scope.data.length; i++) {
                    if ($scope.data[i].category == 'Musician/band') {
                        for (var j=0; j < $scope.artistes.length; j++) {
                            if($scope.artistes[j].facebookId == $scope.data[i].id) {
                                flag = 1;
                                break;
                            }
                        }
                        if(flag == 0) {
                            $scope.artistesFb.push($scope.data[i]);
                            if ($scope.artistesFb.length == 10) {
                                console.log($scope.artistesFb)
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
    };
    $scope.GetArtisteById = function(id){
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBABQgC9YWL113gkHLrA9pgDN38FUK8RJvFAjylDSKtUXHbDLjcLUAlbzh77LNvYwiM8GzCZAes0zUnGEc6y1VchDIXcObwIuPR9GtPEwvvys6irhtDoQKy7ObDe1uHVCm2R7ajSfPKVet2AXMYWVveOw6crs6Oc0ZAh6om5VUYK0HMuxhJGaX9toePrJZAfC1BqmwFZBLeMTAY4gwZBKoZD').
            success(function(data, status, headers, config) {
                console.log(data);
                $scope.artiste = data;
                createArtiste()
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };

    function createArtiste () {
        $http.post('/admin/createArtist', {artistName : $scope.artiste.name, facebookId: $scope.artiste.id}).
            success(function(data, status, headers, config) {
                window.location.href =('#/artiste/' + data);
                console.log(data)
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
    function createPlaces () {
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
        }
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
                    if (data.location.country != 'France') {
                        flag = 1;
                    }
                    if (flag == 0){
                        $http.post('/places', {
                            name: data.name,
                            facebookId: data.id,
                            checkins: data.checkins,
                            cover:data.cover.source,
                            description:data.description,
                            likes:data.likes,
                            link:data.link,
                            location:data.location,
                            phone:data.phone,
                            website:data.website,
                            picture:data.picture
                        })
                    }
                }).
                error(function(data, status, headers, config) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
    } createPlaces()

}]);