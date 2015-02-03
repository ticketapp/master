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
    },
    getEvent : function (id){
    var deferred = $q.defer();
    var events = {};
    var events = factory.getEvents().then(function(events){
        angular.forEach(factory.events, function(value, key){
        if(value.id == id){
            event = value
            }
            });
            deferred.resolve(event);
        }, function(msg){
        deferred.reject(msg);
        });
    return deferred.promise;
    },
    };
    return factory;
});

