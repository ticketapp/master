app.factory ('LieuFactory', function ($http, $q){
		var factory = {
			lieux : false,
            getLieux : function(){
            var deferred = $q.defer();
            if(factory.lieux ==true) {
            deferred.resolve(factory.lieux);
            } else {
                $http.get('/lieux')
                    .success(function(data, status) {
                factory.lieux = data;
                deferred.resolve(factory.lieux);
			}).error(function(data, status) {
				deferred.reject('erreur');
			});
		}
		return deferred.promise;
		},
		getLieu : function (id) {
		var deferred = $q.defer();
		var lieux = {};
		var lieux = factory.getLieux().then(function(lieux) {
			angular.forEach(factory.lieux, function(value, key) {
			if(value.id == id) {
				lieu = value
            }
            });
            deferred.resolve(lieu);
        }, function(msg) {
			deferred.reject(msg);
	    });
		return deferred.promise;
		}
		};
		return factory;
});
	