app.factory ('EventFactory', function ($http, $q){
    var factory = {
        events : false,
    getEvents : function(){
    var deferred = $q.defer();
    if(factory.events == true){
    deferred.resolve(factory.events);
    } else {
    $http.get('/events')
        .success(function(data, status){
            factory.events = data;
            deferred.resolve(factory.events);
        }).error(function(data, status){
            deferred.reject('erreur');
        });
    }
    return deferred.promise;
    }

    };
    return factory;
});

