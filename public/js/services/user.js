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
                    factory.user = 'CAACEdEose0cBAF1NfQ67J84XQTQ6FIAsjZA5SLy328RQe90ZCArFZAmiLHwTfsGRpoIkcPUm7GgBiUQ1VZBFzYDZA2Xb13EeFRCRjS3XJPJAHRAMWzftaKHCQzqNGtucHC1pHt1TStbMWYZBd6xTz8wowZAjySqqKI5mdGkKZBtPfimM06q36aPrFsYEQ6MSbZC9vnjuAAjQzPxWuASP6nOmOeXWdwEGvqKQZD';
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