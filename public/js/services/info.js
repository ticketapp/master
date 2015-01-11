app.factory ('InfoFactory', function ($http, $q){
    var factory = {
        infos : false,
        getInfos : function(){
            var deferred = $q.defer();
            if(factory.infos == true){
                deferred.resolve(factory.infos);
            } else {
                $http.get('/infos')
                .success(function(data, status){
                    factory.infos = data;
                    deferred.resolve(factory.infos);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            }
        return deferred.promise;
        }
    };
    return factory;
});