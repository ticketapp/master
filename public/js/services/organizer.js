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
        },
        followOrganizerByFacebookId : function(id) {
            var deferred = $q.defer();
            if(factory.organizers ==true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.post('/organizers/' + id + '/followByFacebookId ').
                    success(function(data, status, headers, config) {
                        deferred.resolve(data);
                    }).
                    error(function (data) {
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        },
        createOrganizer : function(organizer) {
            var deferred = $q.defer();
            if(factory.organizers ==true) {
                deferred.resolve(factory.organizers);
            } else {
                console.log(organizer)
                $http.post('/organizers/create', organizer).
                    success(function(data, status, headers, config) {
                        deferred.resolve(data);
                    }).error(function (data) {
                        console.log(data)
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        }
    };
    return factory;
});
