angular.module('claudeApp').factory('ArtistsFactory', ['$http', '$q', 'oboe', '$rootScope',
    '$timeout', 'EventsFactory', 'StoreRequest', 'InfoModal', 'ImagesFactory',
    function ($http, $q, oboe, $rootScope, $timeout, EventsFactory, StoreRequest, InfoModal,
              ImagesFactory) {
    var factory = {
        artists : false,
        getArtist : function (url) {
            var deferred = $q.defer();
            $http.get('/artists/' + url)
                .success(function(data, status){
                    data = ImagesFactory(data);
                    factory.artists = data;
                    deferred.resolve(factory.artists);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/artists/' + id + '/isFollowed')
                .success(function(data, status){
                    deferred.resolve(data);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getArtists : function (offset) {
            var deferred = $q.defer();
            $http.get('/artists/since/' + offset + '/12 ')
                .success(function(data, status){
                    data.forEach(ImagesFactory);
                    factory.artists = data;
                    deferred.resolve(factory.artists);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getFollowArtists : function () {
            var deferred = $q.defer();
            $http.get('/artists/followed/')
                .success(function(data, status){
                    data.forEach(ImagesFactory);
                    factory.artists = data;
                    deferred.resolve(factory.artists);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getArtistsByGenre : function (offset, genre) {
            var deferred = $q.defer();
            if(factory.artists ==true){
                deferred.resolve(factory.artists);
            } else {
                $http.get('/genres/' + genre + '/artists?offset=' + offset + '&numberToReturn=12')
                    .success(function(data, status){
                        data.forEach(ImagesFactory);
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
            $http.get('/artists/containing/' + pattern)
                .success(function(data, status){
                    data.forEach(ImagesFactory);
                    factory.artists = data;
                    deferred.resolve(factory.artists);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getArtistsFacebookByContaining : function (pattern) {
            var deferred = $q.defer();
            $http.get('/artists/facebookContaining/'+pattern)
                .success(function(data, status){
                    data.forEach(ImagesFactory);
                    factory.artists = data;
                    deferred.resolve(factory.artists);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        postArtist : function (searchPattern, artist) {
            var deferred = $q.defer();
            oboe.post('artists/createArtist', {
                searchPattern: searchPattern,
                artist: {
                    facebookUrl: artist.facebookUrl,
                    artistName: artist.name,
                    facebookId: artist.facebookId,
                    imagePath: artist.oldImagePath,
                    websites: artist.websites,
                    description: artist.description,
                    genre: artist.genre
                }
            }).start(function (data, etc) {
            })
                .done(function (value) {
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
                            console.log(data)
                        })
                    }
                    value.forEach(saveTrack);
                    deferred.resolve(value);
                })
                .fail(function (error) {
                    deferred.resolve('error');
                    console.log(error)
                });
            return deferred.promise;
        },
        followArtistByFacebookId : function (id) {
            var deferred = $q.defer();
            $http.post('/artists/' + id +'/followByFacebookId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    deferred.resolve('error');
                })
            return deferred.promise;
        },
        followArtistByArtistId : function (id, artistName) {
            var deferred = $q.defer();
            $http.post('/artists/' + id +'/followByArtistId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/artists/' + id +'/followByArtistId', "", 'vous suivez ' + artistName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite');
                    }
                    deferred.resolve('error');
                })
            return deferred.promise;
        },
        unfollowArtist : function (id, artistName) {
            var deferred = $q.defer();
            $http.post('/artists/' + id +'/unfollowArtistByArtistId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/artists/' + id +
                            '/unfollowArtistByArtistId', "", 'vous ne suivez plus' + artistName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite');
                    }
                    deferred.resolve('error');
                })
            return deferred.promise;
        },
        getArtistEvents : function (id) {
            var deferred = $q.defer();
            if(factory.artists == true) {
                deferred.resolve(factory.artists);
            } else {
                $http.get('/artists/'+ id + '/events').
                    success(function (data) {
                        data.forEach(EventsFactory.colorEvent);
                        deferred.resolve(data);
                    }).error(function (data) {
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        },
        getNewArtistTrack : function (artistName, trackTitle, artistFacebookUrl) {
            var deferred = $q.defer();
            if(factory.artists == true) {
                deferred.resolve(factory.artists);
            } else {
                $http.get('/tracks/' + artistName + '/' + artistFacebookUrl + '/' + trackTitle).
                    success(function (data) {
                        deferred.resolve(data);
                    }).error(function (data) {
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        },
        createNewArtistAndPassItToRootScope : function (artist) {
            var searchPattern = document.getElementById('searchBar').value.trim();
            $rootScope.artisteToCreate = true;
            $rootScope.artist = artist;
            $rootScope.tracks = [];
            $rootScope.loadingTracks = true;
            oboe.post('artists/createArtist', {
                searchPattern: searchPattern,
                artist: {
                    facebookUrl: artist.facebookUrl,
                    artistName: artist.name,
                    facebookId: artist.facebookId,
                    imagePath: artist.oldImagePath,
                    websites: artist.websites,
                    description: artist.description,
                    genre: artist.genre
                }
            }).start(function (data, etc) {
            })
            .done(function (value) {
                $timeout(function () {
                    $rootScope.$apply(function () {
                        $rootScope.artist.tracks = $rootScope.artist.tracks.concat(value);
                        $rootScope.tracks = $rootScope.artist.tracks;
                        $timeout(function () {
                            $rootScope.loadingTracks = false;
                        }, 2000)
                    });
                }, 0);
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
                        console.log(data)
                    })
                }
                value.forEach(saveTrack);
            })
            .fail(function (error) {
                console.log(error)
            });
        },
        passArtisteToCreateToFalse : function () {
            $rootScope.artisteToCreate = false;
        }
    };
    return factory;
}]);
