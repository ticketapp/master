angular.module('claudeApp').factory ('UserFactory', ['$http', '$q', 'StoreRequest', 'InfoModal',
    'TracksRecommender', '$rootScope', 'RoutesFactory',
    function ($http, $q, StoreRequest, InfoModal, TracksRecommender, $rootScope, RoutesFactory) {

    var factory = {
        user : {
            name: false,
            id: false,
            isConnected: false,
            facebookAccessToken : false,
            favoritesTracks : [],
            removedTracksIds : [],
            playlists : []
        },
        getToken : function () {
            var deferred = $q.defer();
            $http.get('/users/facebookAccessToken/')
                .success(function(data, status){
                    factory.user.facebookAccessToken = data;
                    deferred.resolve(factory.user.facebookAccessToken);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        deletePlaylist : function(id) {
            var deferred = $q.defer();
            $http.delete('/playlists/' + id).
                success(function (data) {
                    var indexPlaylistToDelet = factory.user.playlists.indexOf(
                        factory.user.playlists.filter(function(playlist) {
                        return playlist.id === id
                    })[0]);
                    factory.user.playlists.splice(indexPlaylistToDelet, 1);
                    deferred.resolve(data);
                }).error (function (data) {
            });
            return deferred.promise;
        },
        ArtistIsFollowed : function(id) {
            var deferred = $q.defer();
            $http.get('/artists/' + id + '/isFollowed').
                success(function (data) {
                    deferred.resolve(data);
                }).error (function (data) {
                deferred.resolve(data)
            });
            return deferred.promise;
        },
        AddTrackToFavorite: function (track) {
            $http.post('/tracks/' + track.uuid + '/addToFavorites').
                success(function () {
                    factory.user.favoritesTracks.push(track);
                    TracksRecommender.UpsertTrackRate(true, track.uuid);

                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/tracks/' + track.uuid + '/addToFavorites', "", 'le moreau a été ajouté à vos favoris')

                        var connectListener = $rootScope.$watch('connected', function (newVal) {
                            if (newVal == true) {
                                TracksRecommender.UpsertTrackRate(true, track.uuid);
                                connectListener();
                            }
                        })
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                })
        },
        removeFromFavorites: function (trackId) {
            $http.post('/tracks/' + trackId + '/removeFromFavorites').
                success(function () {
                    TracksRecommender.UpsertTrackRate(false, trackId);
                    if (factory.user.favoritesTracks) {
                        var FavoritesTracksLength = factory.user.favoritesTracks.length;
                        for (var i = 0; i < FavoritesTracksLength; i++) {
                            if (factory.user.favoritesTracks[i].trackId === trackId) {
                                factory.user.favoritesTracks.splice(i, 1);
                                return;
                            }
                        }
                    }
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/tracks/' + trackId + '/addToFavorites', "", 'le moreau a été ajouté à vos favoris')
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                })
        },
        getFavoritesTracks: function () {
            var deferred = $q.defer();
            if (factory.user.favoritesTracks != false) {
                deferred.resolve(factory.user.favoritesTracks);
            } else {
                $http.get('/tracks/favorites').
                    success(function (data) {
                        factory.user.favoritesTracks = data;
                        deferred.resolve(factory.user.favoritesTracks);
                    }).error(function (data, status) {
                });
            }
            return deferred.promise;
        },
        makeFavoriteTracksRootScope : function () {
            $rootScope.favoritesTracks = [];
            function passFavoritesTracksToRootscope(track) {
                $rootScope.favoritesTracks.push(track.uuid)
            }
            factory.getFavoritesTracks().then(function (tracks) {
                tracks.forEach(passFavoritesTracksToRootscope)
            });
        },
        followPlaceByPlaceId : function (id, placeName) {
            var deferred = $q.defer();
            $http.post('/places/' + id +'/followByPlaceId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/places/' + id +'/followByPlaceId',
                            "", 'vous suivez ' + placeName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        unfollowPlace : function (id, placeName) {
            var deferred = $q.defer();
            $http.post('/places/' + id +'/unfollowPlaceByPlaceId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/places/' + id +'/unfollowPlaceByPlaceId',
                            "", 'vous ne suivez plus ' + placeName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('error');
                });
            return deferred.promise;
        },
        getIsFollowedPlace : function (id) {
            var deferred = $q.defer();
            $http.get('/places/' + id + '/isFollowed')
                .success(function(data, status){
                    factory.user.isFollowedPlace = data;
                    deferred.resolve(factory.events);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        followOrganizerByOrganizerId : function (id, organizerName) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id +'/followByOrganizerId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/organizers/' + id +'/followByOrganizerId',
                            "", 'vous suivez ' + organizerName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        unfollowOrganizer : function (id, organizerName) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id +'/unfollowOrganizerByOrganizerId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/organizers/' + id +'/unfollowOrganizerByOrganizerId',
                            "", 'vous ne suivez plus ' + organizerName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getIsFollowedOrganizer : function (id) {
            var deferred = $q.defer();
            $http.get('/organizers/' + id + '/isFollowed')
                .success(function(data, status){
                    factory.isFollowOrganizer = data;
                    deferred.resolve(factory.events);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        followArtistByArtistId : function (id, artistName) {
            var deferred = $q.defer();
            $http.post('/artists/' + id +'/followByArtistId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data, status) {
                    if (status === 401) {
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
                }).error(function (data, status) {
                    if (status === 401) {
                        StoreRequest.storeRequest('post', '/artists/' + id +
                            '/unfollowArtistByArtistId', "", 'vous ne suivez plus' + artistName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.resolve('error');
                });
            return deferred.promise;
        },
        getRemovedTracks : function () {
            var deferred = $q.defer();
            $http.get(RoutesFactory.user.getRemovedTracks()).success(function (trackIds) {
                factory.user.removedTracksIds = trackIds;
                deferred.resolve(trackIds)
            });
            return deferred.promise;
        }
    };
    return factory;
}]);