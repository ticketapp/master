angular.module('claudeApp').factory ('UserFactory', ['$http', '$q',
    function ($http, $q){
    var factory = {
        user : false,
        getToken : function () {
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.get('/users/facebookAccessToken/')
                    .success(function(data, status){
                        factory.user = 'CAACEdEose0cBAKz5o6MHASZBsZBZCm087PyF9yWrV0qQTg6JyzVXzR6YjXtO8sbQSiJjvOFY4Tcpcn3207aL5M4GG0VukCRZAwmZBiAzfEjAg6EZCeyFY4t2w7qcPZC754ptZAsaU7eTtVsOsfmYs0xcjDqKGVT4Ek60DhQfqlgWE8ZAd3OxgYLsWNfkUqirg1IZBZALdvDZBhmP10qXIWdjmLjsENQpbQSHG70ZD';
                        deferred.resolve(factory.user);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        deletePlaylist : function(id) {
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.delete('/playlists/' + id).
                    success(function (data) {
                        factory.user = data;
                        deferred.resolve(factory.user);
                    }).
                    error (function (data) {
                })
            }
            return deferred.promise;
        },
        ArtistIsFollowed : function(id) {
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.get('/artists/' + id + '/isFollowed').
                    success(function (data) {
                        factory.user = data;
                        deferred.resolve(factory.user);
                    }).
                    error (function (data) {
                    deferred.resolve(data)
                })
            }
            return deferred.promise;
        }
    };
    return factory;
}]);