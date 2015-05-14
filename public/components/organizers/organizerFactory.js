angular.module('claudeApp').factory ('OrganizerFactory',['$http', '$q', 'EventsFactory',
    function ($http, $q, EventsFactory){
    var factory = {
        organizers : false,
        getOrganizer : function(id) {
            var deferred = $q.defer();
            if(factory.organizers == true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.get('/organizers/' + id).
                    success(function(data, status, headers, config) {
                        factory.organizers = data;
                        deferred.resolve(factory.organizers);
                    })
            }
            return deferred.promise;
        },
        getOrganizerEvents : function(id) {
            var deferred = $q.defer();
            if(factory.organizers == true) {
                deferred.resolve(factory.organizers);
            } else {
                $http.get('/organizers/' + id + '/events').
                    success(function(data, status, headers, config) {
                        data.forEach(EventsFactory.colorEvent);
                        factory.organizers = data;
                        deferred.resolve(factory.organizers);
                    })
            }
            return deferred.promise;
        },
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
                $http.post('/organizers/create', organizer).
                    success(function(data, status, headers, config) {
                        deferred.resolve(data);
                    }).error(function (data) {
                        deferred.resolve('error');
                    })
            }
            return deferred.promise;
        }
    };
    return factory;
}]);
