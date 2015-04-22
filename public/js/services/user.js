app.factory ('UserFactory', function ($http, $q){
    var factory = {
        user : false,
        getToken : function(){
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.get('/infos')
                .success(function(data, status){
                    factory.user = data;
                    deferred.resolve(factory.user);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
            }
        return deferred.promise;
        }
    };
    return factory;
});