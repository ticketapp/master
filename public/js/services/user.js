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
                    factory.user = 'CAACEdEose0cBAFRShP5EVANSQCJR3a6uOs97TdHbWFCQd2iVh3Kbx1rfsAOFFwigb6YBncPW0Mqt1Kj4r1C25wdGTGpZAN2bn1QwqnmAQi7TEPdy7pBYDPZBdINVjZChv5FN3K5cWcBZAofTNRBIPdolhHXOMZCj1xTcu9Abvq37IcmUNLhwdZA9xGABEuLj7Vwv7KU0p9pVJZBqXccS48ZClK30EYaldcwZD';
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