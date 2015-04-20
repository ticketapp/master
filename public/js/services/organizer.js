app.factory ('OrganizerFactory', function ($http, $q){
    var factory = {
        organizer : false,
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
