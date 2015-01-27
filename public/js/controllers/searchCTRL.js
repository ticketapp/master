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
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.research + '&limit=500&type=page&access_token=CAACEdEose0cBAPga0OTtvZBY5ZC8N918fJHhJnebTZCisAtlslqUltqZBYSuMZChGFJxbdbeKZBDVivtmrXjS67RIrZCfFChKxNIaI0frSd0Ue0wyr3clfbyZB95XkwGwOhpnMxjY0jbAEbImFiBZAx8CmtWun0FZCyAGBx0ZCIGT3miHsAbFCSM0PKmMEGXZA7sVqU15zgTd7Hclb7KEpdkb4vd7hUNAfe5k0cZD').
            success(function(data, status, headers, config) {
                //$scope.artistesFb = data.data;
                $scope.artistesFb = $filter('filter')(data.data, 'Musician');
                for (var i=0; i < $scope.artistesFb.length; i++) {
                    for (var ii=0; ii < $scope.artistes.length; ii++) {
                        if($scope.artistes[ii].facebookId == $scope.artistesFb[i].id) {
                           delete $scope.artistesFb[i];
                        }
                    }
                }
                console.log($scope.artistesFb)
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };
    $scope.GetArtisteById = function(id){
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAPga0OTtvZBY5ZC8N918fJHhJnebTZCisAtlslqUltqZBYSuMZChGFJxbdbeKZBDVivtmrXjS67RIrZCfFChKxNIaI0frSd0Ue0wyr3clfbyZB95XkwGwOhpnMxjY0jbAEbImFiBZAx8CmtWun0FZCyAGBx0ZCIGT3miHsAbFCSM0PKmMEGXZA7sVqU15zgTd7Hclb7KEpdkb4vd7hUNAfe5k0cZD').
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