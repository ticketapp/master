app.factory ('UserFactory', function ($http, $q){
    var factory = {
        user : false,
        getToken : function () {
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.get('/users/facebookAccessToken/')
                .success(function(data, status){
                    factory.user = 'CAACEdEose0cBANmuxzMMlph7s4DOlKf6SMqUuG0t5nfz3kTlfSviYJm1PonKQ5RgsdJRksvhwXmClUyseo0IK7DWlZALh6Swbx6N4ZBzfkmyPSZBitqNCZCQe1qmJJSi6wv2Q1XZAuIiXbnFzNWUjyWZBBMoi6QQHS2Y50t8aTZCG6lFYgjbl3AAJSK4wTJOEelPbaJuIKek31jLjoBKGAms2MBbbc9MTsZD';
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
});