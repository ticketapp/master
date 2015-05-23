angular.module('claudeApp').factory('ArtistsFactory', ['$http', '$q', 'oboe', '$rootScope',
    '$timeout', 'EventsFactory', 'StoreRequest', 'InfoModal', 'ImagesFactory',
    function ($http, $q, oboe, $rootScope, $timeout, EventsFactory, StoreRequest, InfoModal,
              ImagesFactory) {

    function guid() {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    }

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
                        deferred.reject('erreur');
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
                    deferred.reject('erreur');
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
                        deferred.reject('erreur');
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
                    deferred.reject('erreur');
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
                        deferred.reject('erreur');
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
                        deferred.reject('erreur');
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
                        deferred.reject('erreur');
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
                        track.trackId = guid();
                        $http.post('/tracks/create', {
                            artistFacebookUrl: artist.facebookUrl,
                            redirectUrl : track.redirectUrl,
                            title: track.title,
                            url: track.url,
                            platform: track.platform,
                            thumbnailUrl: track.thumbnailUrl,
                            artistName: track.artistName,
                            trackId: track.trackId
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
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/artists/' + id +'/followByFacebookId', "", '')
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.resolve('error');
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
                    $rootScope.loadingTracks = true;
                    function saveTrack(track) {
                        if (track.redirectUrl == undefined) {
                            track.redirectUrl = track.url;
                        }
                        console.log(track);
                        $http.post('/tracks/create', {
                            trackId: track.trackId,
                            title: track.title,
                            url: track.url,
                            platform: track.platform,
                            thumbnailUrl: track.thumbnailUrl,
                            artistFacebookUrl: track.artistFacebookUrl,
                            artistName: track.artistName,
                            redirectUrl: track.redirectUrl
                        }).success(function (){
                            $rootScope.loadingTracks = false;
                        }).error(function (data) {
                            console.log(data)
                        })
                    }

                    function pushTrack(track) {
                        $timeout(function () {
                            $rootScope.$apply(function () {
                                track.artistName = artist.name;
                                $rootScope.artist.tracks.push(track);
                                $rootScope.tracks.push(track);
                            });
                        }, 0);
                    }

                    function getUuid (track) {
                        track.trackId = guid();
                    }
                    value.forEach(getUuid);
                    //value.forEach(pushTrack);
                    $timeout(function () {
                        $rootScope.$apply(function () {
                            $rootScope.tracks = $rootScope.artist.tracks.concat(value);
                            $rootScope.artist.tracks = $rootScope.artist.tracks.concat(value);
                            $rootScope.loadingTracks = false;
                        })
                    },0);
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
