app.controller('searchCtrl', ['$scope', '$http', function($scope, $http){
    $scope.searchArtist = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.artisteFb + '&limit=200&type=page&access_token=CAACEdEose0cBAKDlvUZC7UZBdUQF2JWgUciuzgpAsnxi7mVttOjCyYjwuKg4BHdXUjz0vaEZAS0rAdfSZBXMBvVA0byk5vdZAjeo0XBsEXIdz8BR8h9kIwREOfXO69duq82AmZAMVKZCCKZAnCg8hpY3MproDQ1VMIdjh0Vq5SBeuIHvaeWTW53FDpeYtMXjqtIxuRFjEXwMW0ZC9w8c2OW5Cm4Wp2FufZCZBEZD').
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
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAKDlvUZC7UZBdUQF2JWgUciuzgpAsnxi7mVttOjCyYjwuKg4BHdXUjz0vaEZAS0rAdfSZBXMBvVA0byk5vdZAjeo0XBsEXIdz8BR8h9kIwREOfXO69duq82AmZAMVKZCCKZAnCg8hpY3MproDQ1VMIdjh0Vq5SBeuIHvaeWTW53FDpeYtMXjqtIxuRFjEXwMW0ZC9w8c2OW5Cm4Wp2FufZCZBEZD').
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