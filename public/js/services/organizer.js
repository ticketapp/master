app.factory ('OrganizerFactory', function ($http, $q){
    var factory = {
        organizer : false,
        getOrganizers : function(offset) {
            var deferred = $q.defer();
            if(factory.users ==true) {
                deferred.resolve(factory.users);
            } else {
                $http.get('/organizers/all/12/' + offset).
                    success(function(data, status, headers, config) {
                        factory.organizer = data;
                        deferred.resolve(factory.organizer);
                    })
            }
            return deferred.promise;
        },
        getOrganizersByContaining : function(pattern) {
            var deferred = $q.defer();
            if(factory.users ==true) {
                deferred.resolve(factory.users);
            } else {
                $http.get('/organizers/containing/'+ pattern).
                    success(function(data, status, headers, config) {
                        factory.organizer = data;
                        deferred.resolve(factory.organizer);
                    })
            }
            return deferred.promise;
        }
    };
    return factory;
});
