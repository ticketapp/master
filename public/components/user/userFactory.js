angular.module('claudeApp').factory ('UserFactory', ['$http', '$q', 'StoreRequest', 'InfoModal',
    'TracksRecommender',
    function ($http, $q, StoreRequest, InfoModal, TracksRecommender){
    var factory = {
        user : false,
        getToken : function () {
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.get('/users/facebookAccessToken/')
                    .success(function(data, status){
                        factory.user = data;
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
        },
        AddTrackToFavorite: function (trackId) {
            $http.post('/tracks/' + trackId + '/addToFavorites').
                success(function () {
                    TracksRecommender.UpsertTrackRate(true, trackId);
                }).
                error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/tracks/' + trackId + '/addToFavorites', "", 'le moreau a été ajouté à vos favoris')
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite');
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
                deferred.resolve(data)
            })
            return deferred.promise;
        }
    };
    return factory;
}]);