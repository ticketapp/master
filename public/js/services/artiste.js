app.factory('ArtistsFactory', function ($http, $q, oboe) {
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
        },
        postArtist : function (searchPattern, artist) {
            var deferred = $q.defer();
            if(factory.artists == true){
                deferred.resolve(factory.artists);
            } else {
                console.log(artist)
                console.log(searchPattern)
                oboe.post('artists/createArtist', {
                    searchPattern: searchPattern,
                    artist: {
                        facebookUrl: artist.facebookUrl,
                        artistName: artist.name,
                        facebookId: artist.facebookId,
                        imagePath: artist.imagePath,
                        websites: artist.websites,
                        description: artist.description,
                        genre: artist.genre
                    }
                }).start(function (data, etc) {
                })
                .done(function (value) {
                    console.log('posted')
                    function saveTrack (track) {
                        if (track.redirectUrl == undefined) {
                            track.redirectUrl = track.url;
                        }
                        $http.post('/tracks/create', {
                            artistFacebookUrl: artist.facebookUrl,
                            redirectUrl : track.redirectUrl,
                            title: track.title,
                            url: track.url,
                            platform: track.platform,
                            thumbnailUrl: track.thumbnailUrl
                        }).error(function (data) {
                        })
                    }
                    value.forEach(saveTrack);
                    deferred.resolve(value);
                })
                .fail(function (error) {
                    deferred.resolve('error');
                    console.log(error)
                });
            }
            return deferred.promise;
        },
        followArtistByFacebookId : function (id) {
            var deferred = $q.defer();
            if(factory.artists == true){
                deferred.resolve(factory.artists);
            } else {
                $http.post('/artists/' + id +'/followByFacebookId').
                    success(function (data) {
                        deferred.resolve(data);
                    }).error(function (data) {
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        }
    };
    return factory;
});
