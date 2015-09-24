angular.module('claudeApp').factory('ArtistsFactory', ['$http', '$q', 'oboe', '$rootScope',
    '$timeout', 'EventsFactory', 'StoreRequest', 'InfoModal', 'ImagesFactory',
    function ($http, $q, oboe, $rootScope, $timeout, EventsFactory, StoreRequest, InfoModal,
              ImagesFactory) {

    var factory = {
        artists : false,
        lastGetArtist: {url: '', artist: {}},
        getArtist : function (url) {
            var deferred = $q.defer();
            if (url == factory.lastGetArtist.url) {
                deferred.resolve(factory.lastGetArtist.artist)
            } else {
                $http.get('/artists/' + url)
                    .success(function (data, status) {
                        data = ImagesFactory(data);
                        factory.lastGetArtist.artist = data;
                        factory.lastGetArtist.url = url;
                        deferred.resolve(factory.lastGetArtist.artist);
                    }).error(function (data, status) {
                        deferred.reject('error');
                    });
            }
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/artists/' + id + '/isFollowed')
                .success(function(data, status){
                    deferred.resolve(data);
                }).error(function(data, status){
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        lastGetArtists: {offset: -1, artists: []},
        getArtists : function (offset) {
            var deferred = $q.defer();
            if (offset <= factory.lastGetArtists.offset) {
                deferred.resolve(factory.lastGetArtists.artists)
            } else {
                $http.get('/artists/since/' + offset + '/12 ')
                    .success(function (data, status) {
                        data.forEach(ImagesFactory);
                        factory.lastGetArtists.artists = factory.lastGetArtists.artists.concat(data);
                        factory.lastGetArtists.offset = offset;
                        deferred.resolve(factory.lastGetArtists.artists);
                    }).error(function (data, status) {
                        deferred.reject('error');
                    });
            }
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
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        lastGetArtistByGenre: {offset: -1, genre: '', artists: []},
        getArtistsByGenre : function (offset, genre) {
            var deferred = $q.defer();
            if(factory.lastGetArtistByGenre.offset >= offset &&
                factory.lastGetArtistByGenre.genre == genre){
                deferred.resolve(factory.lastGetArtistByGenre.artists);
            } else {
                $http.get('/genres/' + genre + '/artists?offset=' + offset + '&numberToReturn=12')
                    .success(function(data, status){
                        data.forEach(ImagesFactory);
                        if (offset > factory.lastGetArtistByGenre.offset) {
                            factory.lastGetArtistByGenre.artists = factory.lastGetArtistByGenre.artists.concat(data);
                        } else {
                            factory.lastGetArtistByGenre.artists = data;
                        }
                        factory.lastGetArtistByGenre.offset = offset;
                        factory.lastGetArtistByGenre.genre = genre;
                        deferred.resolve(factory.lastGetArtistByGenre.artists);
                    }).error(function(data, status){
                        deferred.reject('error');
                    });
            }
            return deferred.promise;
        },
        lastGetArtistsFacebookByContaining: {pattern: '', artists: []},
        getArtistsFacebookByContaining : function (pattern) {
            var deferred = $q.defer();
            if (pattern == factory.lastGetArtistsFacebookByContaining.pattern) {
                if ($rootScope.artist != undefined && $rootScope.artist.facebookUrl.length > 0) {
                    factory.lastGetArtistsFacebookByContaining.artists.splice(
                        factory.lastGetArtistsFacebookByContaining.artists.indexOf($rootScope.artist),
                        1
                    )
                }
                deferred.resolve(factory.lastGetArtistsFacebookByContaining.artists)
            } else {
                $http.get('/artists/facebookContaining/' + pattern)
                    .success(function (data, status) {
                        data.forEach(ImagesFactory);
                        factory.lastGetArtistsFacebookByContaining.artists = data;
                        factory.lastGetArtistsFacebookByContaining.pattern = pattern;
                        deferred.resolve(factory.lastGetArtistsFacebookByContaining.artists);
                    }).error(function (data, status) {
                        deferred.reject('error');
                    });
            }
            return deferred.promise;
        },
        lastGetArtistsByContainig: {pattern: '', artists: []},
        getArtistsByContaining : function (pattern) {
            var deferred = $q.defer();
            if (factory.lastGetArtistsByContainig.pattern == pattern) {
                if ($rootScope.artist != undefined && $rootScope.artist.facebookUrl.length > 0) {
                    factory.lastGetArtistsByContainig.artists.push($rootScope.artist);
                }
                deferred.resolve(factory.lastGetArtistsByContainig.artists)
            } else {
                $http.get('/artists/containing/' + pattern)
                    .success(function (data, status) {
                        data.forEach(ImagesFactory);
                        factory.lastGetArtistsByContainig.artists = data;
                        factory.lastGetArtistsByContainig.pattern = pattern;
                        deferred.resolve(factory.lastGetArtistsByContainig.artists);
                    }).error(function (data, status) {
                        deferred.reject('error');
                    });
            }
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
                    imagePath: artist.imagePath,
                    websites: artist.websites,
                    description: artist.description,
                    genre: artist.genre
                }
            }).start(function (data, etc) {
            })
            .done(function (value) {
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
                }).error(function (data, status) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/artists/' + id +'/followByFacebookId', "", '')
                    }
                    deferred.reject(status);
                });
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
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.resolve('error');
                });
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
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.resolve('error');
                });
            return deferred.promise;
        },
        lastGetArtistEvents: {id: 0 , events: []},
        getArtistEvents : function (id) {
            var deferred = $q.defer();
            if(factory.lastGetArtistEvents.id == id) {
                deferred.resolve(factory.lastGetArtistEvents.events);
            } else {
                $http.get('/artists/'+ id + '/events').
                    success(function (data) {
                        data.forEach(EventsFactory.colorEvent);
                        factory.lastGetArtistEvents.events = data;
                        factory.lastGetArtistEvents.id = id;
                        deferred.resolve(factory.lastGetArtistEvents.events);
                    }).error(function (data) {
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        },
        getNewArtistTrack : function (artistName, trackTitle, artistFacebookUrl) {
            var deferred = $q.defer();
            $http.get('/tracks/' + artistName + '/' + artistFacebookUrl + '/' + trackTitle).
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    deferred.resolve('error');
                });

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
                    $rootScope.loadingTracks = true;
                    /*function pushTrack(track) {
                        $timeout(function () {
                            $rootScope.$apply(function () {
                                $rootScope.artist.tracks.push(track);
                                $rootScope.tracks.push(track);
                                $rootScope.loadingTracks = false;
                            });
                        }, 0);
                    }
                    value.forEach(pushTrack);*/         
                    $timeout(function () {
                        $rootScope.$apply(function () {
                            $rootScope.artist.tracks = $rootScope.artist.tracks.concat(value);
                            $rootScope.tracks = $rootScope.artist.tracks;
                            $rootScope.loadingTracks = false;
                        });
                    }, 0);
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
