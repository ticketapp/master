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
        console.log(typeof $scope.artiste.name);
        $http.get('https://www.googleapis.com/youtube/v3/search?part=snippet&q=' + $scope.artiste.name + '&key=AIzaSyDx-k7jA4V-71I90xHOXiILW3HHL0tkBYc').
            success(function(data, status, headers, config) {
                console.log(data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
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

}]);