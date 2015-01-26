app.controller('searchCtrl', ['$scope', '$http', function($scope, $http){
    $scope.searchArtist = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.artisteFb + '&limit=200&type=page&access_token=CAACEdEose0cBAPGZBgtLAPlr38J9VMiTv5iLG2LFBW4M1tem7itL3Q5rs9hbIBXraom5R0S0izzjtzGnZCAu8CkpoNLSNsee5elvZBOIq8etXMcV2XF4rdiJImEYvADgxGKnyQAwCB250b72MjuW1RZCLbKiw5LQ3d4YsqrhJFMjqriKC1bWIV4KbTLRRZAYdgVw4bhyIVJiZAG9hFvTKOutKZCIARnm4wZD').
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
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAPGZBgtLAPlr38J9VMiTv5iLG2LFBW4M1tem7itL3Q5rs9hbIBXraom5R0S0izzjtzGnZCAu8CkpoNLSNsee5elvZBOIq8etXMcV2XF4rdiJImEYvADgxGKnyQAwCB250b72MjuW1RZCLbKiw5LQ3d4YsqrhJFMjqriKC1bWIV4KbTLRRZAYdgVw4bhyIVJiZAG9hFvTKOutKZCIARnm4wZD').
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