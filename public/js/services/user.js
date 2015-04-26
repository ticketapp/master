app.factory ('UserFactory', function ($http, $q){
    var factory = {
        user : false,
        getToken : function(){
            var deferred = $q.defer();
            if(factory.user == true){
                deferred.resolve(factory.user);
            } else {
                $http.get('/users/facebookAccessToken/')
                .success(function(data, status){
                    factory.user = 'CAACEdEose0cBAHP9QwrU5V54IpE3JTn0J7ZCy6Q2BMVK48nybH8Nq21fcdZCFVIsvRrbK2hBwLScbb0xcF22bxyQdd3aM7nrHnYEtZBqDphph2TafoG71MtONJk4dDzGDgtLBT35g0CtAboGIS9yeYLDxt0CKxttGORUNAvJTeeXRSeYTaJcW99wc3rQ3B3MaPmnyxIpLPgbUZAbWaz0x8wu8ZCqLVJwZD';
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