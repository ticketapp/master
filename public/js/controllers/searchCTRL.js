app.controller('searchCtrl', ['$scope', '$http', function($scope, $http){
    $scope.searchArtist = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.artisteFb + '&limit=200&type=page&access_token=CAACEdEose0cBAOoLxnzoaILohnySbcAaz0roZBwN64prZAlmwBkh93UZCcHwVDNQFA0Jqte5HeOFFdQKglpxg8m2S9ENDaXyjApCNL80VE5QS7JLZApkwbXPYKCZBBjhmc7K6D7uZBtIaQOVSm5M2hNka9SeVZBYjHJyKuRFFiAUhASKahg7wJp3LGMocRjgZBXcj5hWqFb0dRq7a6Hn2C3aybdX6AqMXxAZD').
            success(function(data, status, headers, config) {
                console.log(data);
                $scope.artistes = data.data;
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };
    $scope.GetArtisteById = function(id){
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAOoLxnzoaILohnySbcAaz0roZBwN64prZAlmwBkh93UZCcHwVDNQFA0Jqte5HeOFFdQKglpxg8m2S9ENDaXyjApCNL80VE5QS7JLZApkwbXPYKCZBBjhmc7K6D7uZBtIaQOVSm5M2hNka9SeVZBYjHJyKuRFFiAUhASKahg7wJp3LGMocRjgZBXcj5hWqFb0dRq7a6Hn2C3aybdX6AqMXxAZD').
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
        $http.post('/admin/createArtist', {artistName : $scope.artiste.name}).
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