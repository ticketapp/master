angular.module('claudeApp').factory ('UserFactory', ['$http', '$q', 'StoreRequest', 'InfoModal',
    'TracksRecommender', '$rootScope',
    function ($http, $q, StoreRequest, InfoModal, TracksRecommender, $rootScope){
    var factory = {
        user : false,
        getToken : function () {
            var deferred = $q.defer();
            $http.get('/users/facebookAccessToken/')
                .success(function(data, status){
                    factory.user = 'CAACEdEose0cBALAa19O7X004YOuxGFe2lqsX1xulrOa46qulSYkKKmiJvJWX8LTsbu5j7fAEz9oaWOls0N2I5Ujtl00TdgWybjMYy0ZCyGXZBqlPPZAOqJoxUbTWOhGmTy7thKoxPQrEU9qJtv6HeCEknB3I87bKvhesce2vUwzFRTt7AA8ifZB6LgzaesGBZC7nXwMidu91F0pWuKCElC4FD1r9u6joZD';
                    deferred.resolve(factory.user);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        deletePlaylist : function(id) {
            var deferred = $q.defer();
            $http.delete('/playlists/' + id).
                success(function (data) {
                    factory.user = data;
                    deferred.resolve(factory.user);
                }).
                error (function (data) {
            });
            return deferred.promise;
        },
        ArtistIsFollowed : function(id) {
            var deferred = $q.defer();
            $http.get('/artists/' + id + '/isFollowed').
                success(function (data) {
                    factory.user = data;
                    deferred.resolve(factory.user);
                }).
                error (function (data) {
                deferred.resolve(data)
            });
            return deferred.promise;
        },
        AddTrackToFavorite: function (trackId) {
            $http.post('/tracks/' + trackId + '/addToFavorites').
                success(function () {
                    TracksRecommender.UpsertTrackRate(true, trackId);
                }).
                error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/tracks/' + trackId + '/addToFavorites', "", 'le moreau a été ajouté à vos favoris')
                        var connectListener = $rootScope.$watch('connected', function (newVal) {
                            if (newVal == true) {
                                TracksRecommender.UpsertTrackRate(true, trackId);
                                connectListener();
                            }
                        })
                    } else {
                        console.log(data)
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                })
        },
        removeFromFavorites: function (trackId) {
            $http.post('/tracks/' + trackId + '/removeFromFavorites').
                success(function () {
                    TracksRecommender.UpsertTrackRate(false, trackId);
                }).
                error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/tracks/' + trackId + '/addToFavorites', "", 'le moreau a été ajouté à vos favoris')
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                })
        },
        getFavoritesTracks: function () {
            var deferred = $q.defer();
            $http.get('/tracks/favorites').
                success(function (data) {
                    factory.user = data;
                    deferred.resolve(factory.user);
                }).
                error (function (data) {
            });
            return deferred.promise;
        },
        makeFavoriteTracksRootScope : function () {
            $rootScope.favoritesTracks = [];
            function passFavoritesTracksToRootscope(track) {
                $rootScope.favoritesTracks.push(track.trackId)
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
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
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
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
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
                    factory.events = data;
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
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
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
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
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
                    factory.events = data;
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
        }
    };
    return factory;
}]);