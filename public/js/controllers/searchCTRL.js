app.controller('searchCtrl', ['$scope', '$http', function($scope, $http){
    $scope.searchArtist = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.artisteFb + '&limit=200&type=page&access_token=CAACEdEose0cBAGXo1iKWZCN8wIYzYmkZByVkZBSaoeinH39RcYyBwO5fkDHsCJerDZBGuZBma7xL9mVK42vRx2SDV0ZBfSds1mWMqQREW8bhjFfJV8w2WFm4AiZAJZCXGnFHkmZCJfj44GHcOFagM9U4j09V5MB0LiC8FFxOULFWUsTvBJlsI0RxovNHvJlZBC2xdfR0xvwy5Gl8w6rrfI5rz1B4eI2fArABAZD').
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
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAGXo1iKWZCN8wIYzYmkZByVkZBSaoeinH39RcYyBwO5fkDHsCJerDZBGuZBma7xL9mVK42vRx2SDV0ZBfSds1mWMqQREW8bhjFfJV8w2WFm4AiZAJZCXGnFHkmZCJfj44GHcOFagM9U4j09V5MB0LiC8FFxOULFWUsTvBJlsI0RxovNHvJlZBC2xdfR0xvwy5Gl8w6rrfI5rz1B4eI2fArABAZD').
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
                // this callback will be called asynchronously
                // when the response is available
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }

}]);