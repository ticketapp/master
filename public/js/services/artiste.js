app.factory('ArtistsFactory', function ($http, $q) {
    var factory = {
        artists : false,
        getArtists : function (offset) {
            var deferred = $q.defer();
            if(factory.artists ==true){
                deferred.resolve(factory.artists);
            } else {
                $http.get('/artists/since/' + offset + '/12 ')
                    .success(function(data, status){
                        factory.artists = data;
                        deferred.resolve(factory.artists);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        getArtistsByGenre : function (offset, genre) {
            var deferred = $q.defer();
            if(factory.artists ==true){
                deferred.resolve(factory.artists);
            } else {
                $http.get('/genres/' + genre + '/12/' + offset + '/artists')
                    .success(function(data, status){
                        factory.artists = data;
                        deferred.resolve(factory.artists);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        getArtistsByContaining : function (pattern) {
            var deferred = $q.defer();
            if(factory.artists ==true){
                deferred.resolve(factory.artists);
            } else {
                $http.get('/artists/containing/' + pattern)
                    .success(function(data, status){
                        factory.artists = data;
                        deferred.resolve(factory.artists);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        },
        getArtistsFacebookByContaining : function (pattern) {
            var deferred = $q.defer();
            if(factory.artists == true){
                deferred.resolve(factory.artists);
            } else {
                $http.get('/artists/facebookContaining/'+pattern)
                    .success(function(data, status){
                        factory.artists = data;
                        deferred.resolve(factory.artists);
                    }).error(function(data, status){
                        deferred.reject('erreur');
                    });
            }
            return deferred.promise;
        }
    };
    return factory;
});
