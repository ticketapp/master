angular.module('claudeApp').factory ('OrganizerFactory',['$http', '$q', 'EventsFactory', 'StoreRequest',
    'InfoModal',
    function ($http, $q, EventsFactory, StoreRequest, InfoModal){
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
                $http.get('/organizers?numberToReturn=12&offset='+offset).
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
        followOrganizerByOrganizerId : function (id, organizerName) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id +'/followByOrganizerId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/organizers/' + id +'/followByOrganizerId',
                            "", 'vous suivez ' + organizerName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        unfollowOrganizer : function (id, organizerName) {
            var deferred = $q.defer();
            $http.post('/organizers/' + id +'/unfollowOrganizerByOrganizerId').
                success(function (data) {
                    deferred.resolve(data);
                }).error(function (data) {
                    if (data.error == 'Credentials required') {
                        StoreRequest.storeRequest('post', '/organizers/' + id +'/unfollowOrganizerByOrganizerId',
                            "", 'vous ne suivez plus ' + organizerName)
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                    deferred.reject('erreur');
                });
            return deferred.promise;
        },
        getIsFollowed : function (id) {
            var deferred = $q.defer();
            $http.get('/organizers/' + id + '/isFollowed')
                .success(function(data, status){
                    factory.events = data;
                    deferred.resolve(factory.events);
                }).error(function(data, status){
                    deferred.reject('erreur');
                });
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
