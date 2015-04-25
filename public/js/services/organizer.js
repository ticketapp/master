app.factory ('OrganizerFactory', function ($http, $q){
    var factory = {
        organizers : false,
        getOrganizers : function(offset) {
            var deferred = $q.defer();
            if(factory.organizers == true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.get('/organizers/all/12/' + offset).
                    success(function(data, status, headers, config) {
                        factory.organizers = data;
                        deferred.resolve(factory.organizers);
                    })
            }
            return deferred.promise;
        },
        getOrganizersByContaining : function(pattern) {
            var deferred = $q.defer();
            if(factory.organizers ==true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.get('/organizers/containing/'+ pattern).
                    success(function(data, status, headers, config) {
                        factory.organizers = data;
                        deferred.resolve(factory.organizers);
                    })
            }
            return deferred.promise;
        }
    };
    return factory;
});
